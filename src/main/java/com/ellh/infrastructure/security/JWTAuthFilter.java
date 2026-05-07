package com.ellh.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs once per request.
 * Extracts Bearer token from Authorization header, validates it via JWTService,
 * and sets the authentication in the SecurityContext.
 *
 * Section 4.5.5.5 — API Authentication enforcement point.
 * Positioned in the Spring Security filter chain via SecurityConfig:
 *   http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
 *
 * Filter chain position:
 *   CorsFilter → SecurityContextPersistenceFilter → JWTAuthFilter
 *   → RateLimitFilter → UsernamePasswordAuthenticationFilter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        log.debug("Auth header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        log.debug("JWT token: {}...", jwt.substring(0, Math.min(jwt.length(), 30)));

        try {
            final String userEmail = jwtService.extractUsername(jwt); // <-- make sure this method exists
            log.info("Extracted email from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.info("Loaded user: {}, authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

                boolean isValid = jwtService.isTokenValid(jwt, userDetails);
                log.info("isTokenValid result: {}", isValid);

                if (isValid) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authentication set for {}", userEmail);
                } else {
                    log.warn("JWT token INVALID for user {}", userEmail);
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication error", e);
        }

        filterChain.doFilter(request, response);
    }
}
