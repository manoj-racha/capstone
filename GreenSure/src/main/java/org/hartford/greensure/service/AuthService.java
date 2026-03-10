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

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private HouseholdProfileRepository householdProfileRepository;
    @Autowired private MsmeProfileRepository msmeProfileRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private CarbonDeclarationRepository declarationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuthenticationManager authenticationManager;

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

    public void forgotPassword(ForgotPasswordRequest request) {
        boolean existsInUsers = userRepository.existsByEmail(request.getEmail());
        boolean existsInAgents = agentRepository.existsByEmail(request.getEmail());

        if (!existsInUsers && !existsInAgents) {
            return;
        }

        String resetToken = jwtUtil.generateToken(0L, request.getEmail(), "RESET");

        System.out.println("PASSWORD RESET TOKEN for " + request.getEmail() + ": " + resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!jwtUtil.isTokenValid(request.getToken())) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        String email = jwtUtil.extractEmail(request.getToken());
        String newHash = passwordEncoder.encode(request.getNewPassword());

        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(newHash);
            userRepository.save(user);
        });

        agentRepository.findByEmail(email).ifPresent(agent -> {
            agent.setPasswordHash(newHash);
            agentRepository.save(agent);
        });
    }
}
