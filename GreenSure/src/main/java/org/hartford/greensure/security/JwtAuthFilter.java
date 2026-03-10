package org.hartford.greensure.security;



import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1 — Read Authorization header
        String authHeader = request.getHeader("Authorization");

        // Step 2 — If no header or not Bearer token — skip filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3 — Extract token from header
        // Header format: "Bearer eyJhbGci..."
        String token = authHeader.substring(7);

        // Step 4 — Validate token
        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 5 — Extract email from token
        String email = jwtUtil.extractEmail(token);

        // Step 6 — Only set authentication if not already set
        if (email != null &&
                SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            try {
                // Step 7 — Load user details from database
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                // Step 8 — Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // Step 9 — Set authentication in security context
                // From this point Spring Security knows who this is
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            } catch (Exception e) {
                // If user not found or any error, just continue without authentication
                // Spring Security will handle authorization at controller level
            }
        }

        // Step 10 — Continue to next filter or controller
        filterChain.doFilter(request, response);
    }
}
