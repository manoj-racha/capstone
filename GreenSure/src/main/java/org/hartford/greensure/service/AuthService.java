package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.exception.AgentNotFoundException;
import org.hartford.greensure.exception.BadRequestException;
import org.hartford.greensure.exception.InvalidTokenException;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HouseholdProfileRepository householdProfileRepository;
    @Autowired
    private MsmeProfileRepository msmeProfileRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private CarbonDeclarationRepository declarationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByMobile(request.getMobile())) {
            throw new BadRequestException("Mobile number already registered");
        }

        if (request.getUserType() == User.UserType.HOUSEHOLD) {
            if (request.getNumberOfMembers() == null) {
                throw new BadRequestException("Number of members is required for household");
            }
            if (request.getDwellingType() == null) {
                throw new BadRequestException("Dwelling type is required for household");
            }
        }

        MsmeProfile.BusinessType parsedBusinessType = null;

        if (request.getUserType() == User.UserType.MSME) {
            if (request.getBusinessName() == null || request.getBusinessName().isBlank()) {
                throw new BadRequestException("Business name is required for MSME");
            }
            if (request.getGstNumber() == null || request.getGstNumber().isBlank()) {
                throw new BadRequestException("GST number is required for MSME");
            }
            if (!GST_PATTERN.matcher(request.getGstNumber().trim().toUpperCase()).matches()) {
                throw new BadRequestException("Invalid GST number format");
            }
            if (request.getBusinessType() == null || request.getBusinessType().isBlank()) {
                throw new BadRequestException("Business type is required for MSME");
            }
            parsedBusinessType = parseBusinessType(request.getBusinessType());
            if (request.getNumEmployees() == null) {
                throw new BadRequestException("Number of employees is required for MSME");
            }
            if (msmeProfileRepository.existsByGstNumber(request.getGstNumber())) {
                throw new BadRequestException("GST number already registered");
            }
        }

        User user = User.builder()
                .userType(request.getUserType())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .pinCode(request.getPinCode())
                .city(request.getCity())
                .state(request.getState())
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        if (request.getUserType() == User.UserType.HOUSEHOLD) {
            HouseholdProfile profile = HouseholdProfile.builder()
                    .user(user)
                    .numberOfMembers(request.getNumberOfMembers())
                    .dwellingType(request.getDwellingType())
                    .build();
            householdProfileRepository.save(profile);
        } else {
            MsmeProfile profile = MsmeProfile.builder()
                    .user(user)
                    .businessName(request.getBusinessName())
                    .gstNumber(request.getGstNumber())
                    .businessType(parsedBusinessType)
                    .numEmployees(request.getNumEmployees())
                    .build();
            msmeProfileRepository.save(profile);
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), "USER");

        return AuthResponse.builder()
                .token(token)
                .role("USER")
                .userType(user.getUserType())
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .isFirstLogin(true)
                .build();
    }

    private MsmeProfile.BusinessType parseBusinessType(String rawBusinessType) {
        try {
            return MsmeProfile.BusinessType.valueOf(rawBusinessType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid business type. Allowed values: MANUFACTURING, RETAIL, SERVICE, FOOD");
        }
    }

    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new BadRequestException("Your account has been suspended. Contact support.");
        }

        boolean isFirstLogin = !declarationRepository.existsByUserUserId(user.getUserId());

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), "USER");

        return AuthResponse.builder()
                .token(token)
                .role("USER")
                .userType(user.getUserType())
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .isFirstLogin(isFirstLogin)
                .build();
    }

    public AuthResponse loginAgent(LoginRequest request) {
        Agent agent = agentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AgentNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), agent.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        if (agent.getStatus() == Agent.AgentStatus.INACTIVE) {
            throw new BadRequestException("Your account is inactive. Contact admin.");
        }

        String role = agent.getAgentType() == Agent.AgentType.ADMIN ? "ADMIN" : "AGENT";

        String token = jwtUtil.generateToken(agent.getAgentId(), agent.getEmail(), role);

        return AuthResponse.builder()
                .token(token)
                .role(role)
                .id(agent.getAgentId())
                .fullName(agent.getFullName())
                .email(agent.getEmail())
                .isFirstLogin(false)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            try {
                emailService.sendEmail(
                        user.getEmail(),
                        "Reset Your GreenSure Password",
                        "<p>You requested a password reset for your GreenSure account.</p>"
                                + "<p>Click the link below to reset your password. This link expires in 15 minutes.</p>"
                                + "<p><a href=\"" + resetLink
                                + "\" style=\"display:inline-block;padding:10px 16px;background:#1f5c3a;color:#ffffff;text-decoration:none;border-radius:6px;\">Reset Password</a></p>"
                                + "<p>If you did not request this, ignore this email.</p>"
                                + "<p>Your password will not be changed.</p>");
            } catch (RuntimeException ex) {
                // Do not leak mail server failures to API clients.
                log.error("Forgot-password email send failed for {}: {}", user.getEmail(), ex.getMessage());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset link."));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset link has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        emailService.sendEmail(
                user.getEmail(),
                "Your Password Has Been Reset — GreenSure",
                "<p>Your GreenSure password has been successfully reset.</p>"
                        + "<p>If you did not do this, contact support immediately.</p>");
    }
}
