package org.hartford.greensure.security;

import org.hartford.greensure.entity.User;
import org.hartford.greensure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads UserDetails from the single unified User table.
 * All roles (USER, AGENT, ADMIN) are stored as User entities with different
 * role values.
 * The old Agent entity has been removed; its authorities come from
 * User.getAuthorities().
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with email: " + email));

        String role;
        if (user.getRole() != null) {
            role = user.getRole().name();
        } else if (user.getUserType() != null) {
            role = switch (user.getUserType()) {
                case ADMIN -> "ADMIN";
                case AGENT -> "AGENT";
                default -> "USER";
            };
        } else {
            role = "USER";
        }
        return new SecurityUser(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
