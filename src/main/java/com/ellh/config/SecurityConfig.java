package com.ellh.config;

import com.ellh.infrastructure.security.JWTAuthFilter;
import com.ellh.infrastructure.security.RateLimitFilter;
import com.ellh.user.entity.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the ELLH API.
 * Section 4.5.5.5 — all 10 security enforcement points.
 * Section 4.4 SS-2 — RBAC and CORS policy.
 *
 * Key decisions:
 * - Stateless JWT (no HTTP sessions)
 * - CSRF disabled (JWT-based API, no cookie auth)
 * - CORS restricted to ELLH Android package origin
 * - RateLimitFilter injected AFTER JWTAuthFilter (uses the real class name)
 * - Role-based access: CONTENT_ADMIN and SYSTEM_ADMIN for admin endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;   // ← injected by Spring; class is RateLimitFilter
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Stateless API: no sessions, no CSRF ──────────────────────────
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: allow only the ELLH Android app origin ─────────────────
            // Android apps present their package name as the origin.
            // Additional origins (web dashboard) can be added here in future.
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowedOrigins(java.util.List.of("https://ellh.app", "http://localhost"));
                config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
                config.setAllowedHeaders(java.util.List.of("*"));
                config.setExposedHeaders(java.util.List.of(
                        "X-Rate-Limit-Remaining", "Retry-After"));
                return config;
            }))

            // ── Endpoint authorisation rules ─────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no JWT required
                .requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()

                // ContentAdmin + SystemAdmin only
                .requestMatchers(HttpMethod.POST,   "/api/v1/lessons/**").hasAnyRole(
                        UserType.CONTENT_ADMIN.name(), UserType.SYSTEM_ADMIN.name())
                .requestMatchers(HttpMethod.PUT,    "/api/v1/lessons/**").hasAnyRole(
                        UserType.CONTENT_ADMIN.name(), UserType.SYSTEM_ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/v1/lessons/**").hasAnyRole(
                        UserType.CONTENT_ADMIN.name(), UserType.SYSTEM_ADMIN.name())
                .requestMatchers(HttpMethod.POST,   "/api/v1/contrastive/rules").hasAnyRole(
                        UserType.CONTENT_ADMIN.name(), UserType.SYSTEM_ADMIN.name())
                .requestMatchers("/api/v1/feedback/admin/**").hasAnyRole(
                        UserType.CONTENT_ADMIN.name(), UserType.SYSTEM_ADMIN.name())

                // All other /api/v1/** endpoints require authentication
                .requestMatchers("/api/v1/**").authenticated()

                // Deny everything else
                .anyRequest().denyAll()
            )

            // ── Filter chain order ────────────────────────────────────────────
            // JWTAuthFilter runs before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // RateLimitFilter runs AFTER JWTAuthFilter so userId is available
            // Uses the real class name JWTAuthFilter (not JwtAuthenticationFilter)
            .addFilterAfter(rateLimitFilter, JWTAuthFilter.class)

            // ── Custom auth provider ──────────────────────────────────────────
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — secure default for 2024 hardware
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
