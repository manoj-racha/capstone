package org.hartford.greensure.security;



import org.hartford.greensure.entity.Agent;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.repository.AgentRepository;
import org.hartford.greensure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;

    // ── Load by email — tries User first then Agent ────────────

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // Try finding in users table first
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return buildUserDetails(
                    user.getUserId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    "ROLE_USER"
            );
        }

        // Try finding in agents table
        Agent agent = agentRepository.findByEmail(email).orElse(null);
        if (agent != null) {
            String role = agent.getAgentType() == Agent.AgentType.ADMIN
                    ? "ROLE_ADMIN"
                    : "ROLE_AGENT";
            return buildUserDetails(
                    agent.getAgentId(),
                    agent.getEmail(),
                    agent.getPasswordHash(),
                    role
            );
        }

        throw new UsernameNotFoundException(
                "No account found with email: " + email);
    }

    // ── Private helper to build UserDetails ───────────────────

    private UserDetails buildUserDetails(
            Long id, String email, String passwordHash, String role) {
        return new SecurityUser(
                id,
                email,
                passwordHash,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
