package org.hartford.greensure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter.
 * Extracts the token from the Authorization header, validates it, and
 * sets the authentication in the SecurityContext.
 *
 * Updated: the principal stored is the user ID (Long) so controllers can
 * call getPrincipal() to get the ID directly.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                Long id = jwtUtil.extractId(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Reject stale tokens for deleted/suspended users and role-changed accounts.
                    var dbUserOpt = userRepository.findById(id)
                            .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                            .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                            .filter(u -> {
                                String dbRole = u.getRole() != null ? u.getRole().name() : null;
                                return dbRole != null && dbRole.equalsIgnoreCase(role);
                            });

                    if (dbUserOpt.isEmpty()) {
                        filterChain.doFilter(request, response);
                        return;
                    }

                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var principal = new SecurityUser(id, email, "", List.of(authority));
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal, // principal = SecurityUser so @AuthenticationPrincipal works
                            null,
                            List.of(authority));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
