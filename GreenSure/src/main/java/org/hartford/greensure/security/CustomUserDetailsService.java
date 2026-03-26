package org.hartford.greensure.security;

import org.hartford.greensure.entity.User;
import org.hartford.greensure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * Loads UserDetails from the single unified User table.
 * All roles (USER, AGENT, ADMIN) are stored as User entities with different role values.
 * The old Agent entity has been removed; its authorities come from User.getAuthorities().
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found with email: " + email));
    }
}
