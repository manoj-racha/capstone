package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HouseholdProfileRepository householdProfileRepository;

    @Autowired
    private CarbonDeclarationRepository declarationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private EmailService emailService;
    @Autowired
    private OtpService otpService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public String register(RegisterRequest request) {
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

        User user = User.builder()
                .userType(request.getUserType())
                .role(mapRole(request.getUserType()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .pinCode(request.getPinCode())
                .city(request.getCity())
                .state(request.getState())
                .status(User.UserStatus.INACTIVE)
                .build();

        user = userRepository.save(user);

        if (request.getUserType() == User.UserType.HOUSEHOLD) {
            HouseholdProfile profile = HouseholdProfile.builder()
                    .user(user)
                    .numberOfMembers(request.getNumberOfMembers())
                    .dwellingType(request.getDwellingType())
                    .build();
            householdProfileRepository.save(profile);
        }

        sendRegistrationOtp(user.getEmail());
        return "OTP sent to your email. Please verify to activate your account.";
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

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new BadRequestException("Please verify your email with OTP before logging in.");
        }

        boolean isFirstLogin = !declarationRepository.existsByUserUserId(user.getUserId());

        String role = resolveRole(user);

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), role);

        return AuthResponse.builder()
                .token(token)
                .role(role)
                .userType(user.getUserType())
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .isFirstLogin(isFirstLogin)
                .build();
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        otpService.validateOtp(request.getEmail(), request.getOtp());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found for this email."));

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new BadRequestException("Your account has been suspended. Contact support.");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            user.setStatus(User.UserStatus.ACTIVE);
            userRepository.save(user);
        }

        boolean isFirstLogin = !declarationRepository.existsByUserUserId(user.getUserId());
        String role = resolveRole(user);
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), role);

        return AuthResponse.builder()
                .token(token)
                .role(role)
                .userType(user.getUserType())
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .isFirstLogin(isFirstLogin)
                .emailVerified(true)
                .build();
    }

    @Transactional(readOnly = true)
    public void resendOtp(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == User.UserStatus.INACTIVE) {
                sendRegistrationOtp(user.getEmail());
            }
        });
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

        if (user.getPasswordResetTokenExpiry() == null
                || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
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

    private String resolveRole(User user) {
        if (user.getRole() != null) {
            return user.getRole().name();
        }
        return mapRole(user.getUserType()).name();
    }

    private User.Role mapRole(User.UserType userType) {
        if (userType == null) {
            return User.Role.USER;
        }
        return switch (userType) {
            case ADMIN -> User.Role.ADMIN;
            case AGENT -> User.Role.AGENT;
            default -> User.Role.USER;
        };
    }

    private void sendRegistrationOtp(String email) {
        String otp = otpService.generateAndStoreOtp(email);
        try {
            emailService.sendEmail(
                    email,
                    "Verify Your GreenSure Account",
                    "<p>Welcome to GreenSure.</p>"
                            + "<p>Your verification code is:</p>"
                            + "<p style=\"font-size:24px;font-weight:700;letter-spacing:4px;margin:12px 0;\">" + otp
                            + "</p>"
                            + "<p>This code expires in 10 minutes.</p>");
        } catch (RuntimeException ex) {
            log.error("Failed to send registration OTP to {}: {}", email, ex.getMessage());
            throw new BadRequestException("Unable to send verification email right now. Please try again in a moment.");
        }
    }
}
