package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

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
    private AuthenticationManager authenticationManager;
    @Autowired
    private EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByMobile(request.getMobile())) {
            throw new RuntimeException("Mobile number already registered");
        }

        if (request.getUserType() == User.UserType.HOUSEHOLD) {
            if (request.getNumberOfMembers() == null) {
                throw new RuntimeException("Number of members is required for household");
            }
            if (request.getDwellingType() == null) {
                throw new RuntimeException("Dwelling type is required for household");
            }
        }

        if (request.getUserType() == User.UserType.MSME) {
            if (request.getBusinessName() == null) {
                throw new RuntimeException("Business name is required for MSME");
            }
            if (msmeProfileRepository.existsByGstNumber(request.getGstNumber())) {
                throw new RuntimeException("GST number already registered");
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
                    .businessType(request.getBusinessType())
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

    public AuthResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw new RuntimeException("Your account has been suspended. Contact support.");
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
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), agent.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (agent.getStatus() == Agent.AgentStatus.INACTIVE) {
            throw new RuntimeException("Your account is inactive. Contact admin.");
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
        // Always return success to prevent email enumeration
        String email = request.getEmail();

        // Check users table
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, resetToken);
        });

        // Check agents table
        agentRepository.findByEmail(email).ifPresent(agent -> {
            // For agents, use JWT-based token (no resetToken field on Agent entity)
            String resetToken = jwtUtil.generateToken(0L, email, "RESET");
            emailService.sendPasswordResetEmail(email, resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String newHash = passwordEncoder.encode(request.getNewPassword());

        // Try DB-backed token first (for users)
        var userOpt = userRepository.findByResetToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Reset link has expired. Please request a new one.");
            }
            user.setPasswordHash(newHash);
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            return;
        }

        // Fallback: try JWT-based token (for agents)
        if (jwtUtil.isTokenValid(token)) {
            String email = jwtUtil.extractEmail(token);
            agentRepository.findByEmail(email).ifPresent(agent -> {
                agent.setPasswordHash(newHash);
                agentRepository.save(agent);
            });
        } else {
            throw new RuntimeException("Invalid or expired reset token.");
        }
    }
}
