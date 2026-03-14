package org.hartford.greensure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private UserDetailsService userDetailsService;

        // ── Public and Protected Endpoint Rules ────────────────────

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {

                http
                                // Enable CORS globally
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Disable CSRF — not needed for stateless REST APIs
                                .csrf(AbstractHttpConfigurer::disable)

                                // Stateless session — no server-side sessions
                                // Every request must carry its own JWT token
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Endpoint access rules
                                .authorizeHttpRequests(auth -> auth

                                                // ── PUBLIC ENDPOINTS — No token required ───────
                                                .requestMatchers(
                                                                "/auth/register",
                                                                "/auth/login",
                                                                "/auth/forgot-password",
                                                                "/auth/reset-password")
                                                .permitAll()

                                                // ── H2 CONSOLE — Development only ─────────────
                                                .requestMatchers("/h2-console/**").permitAll()

                                                // ── SWAGGER UI — API Documentation ─────────────
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/v3/api-docs")
                                                .permitAll()

                                                // ── USER ENDPOINTS — ROLE_USER only ───────────
                                                .requestMatchers(
                                                                "/user/**",
                                                                "/declaration/**",
                                                                "/score/**",
                                                                "/recommendations/**")
                                                .hasRole("USER")

                                                // ── AGENT ENDPOINTS — ROLE_AGENT only ─────────
                                                .requestMatchers(
                                                                "/agent/**")
                                                .hasRole("AGENT")

                                                // ── ADMIN ENDPOINTS — ROLE_ADMIN only ─────────
                                                .requestMatchers(
                                                                "/admin/**")
                                                .hasRole("ADMIN")

                                                // ── NOTIFICATIONS — USER and AGENT ────────────
                                                .requestMatchers(
                                                                "/notifications/**")
                                                .hasAnyRole("USER", "AGENT", "ADMIN")

                                                // All other endpoints require authentication
                                                .anyRequest().authenticated())

                                // Register JWT filter before Spring's default
                                // password filter
                                .addFilterBefore(
                                                jwtAuthFilter,
                                                UsernamePasswordAuthenticationFilter.class)

                                // Allow H2 console frames in browser
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.sameOrigin()));

                return http.build();
        }

        // ── Password Encoder — BCrypt ──────────────────────────────

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // ── Authentication Provider ────────────────────────────────

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        // ── Authentication Manager ─────────────────────────────────

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // Allow the Angular frontend URL
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200", "http://localhost:4201"));
                // Allow standard HTTP methods
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                // Allow necessary headers
                configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With",
                                "accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
                // Allow exposition of headers
                configuration.setExposedHeaders(
                                Arrays.asList("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
                // Allow credentials (like cookies or auth headers, though we use Bearer tokens)
                configuration.setAllowCredentials(true);
                // Apply the config to all paths
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
