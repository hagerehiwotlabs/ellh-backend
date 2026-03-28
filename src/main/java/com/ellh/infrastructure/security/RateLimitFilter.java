package com.ellh.infrastructure.security;

import com.ellh.infrastructure.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiting filter using Bucket4j token buckets (in-memory).
 * Section 4.5.5.5 — Rate Limiting enforcement point.
 * Section 4.4 SS-2 — "Spring Security rate limiter enforces per-user request
 * limits on AI endpoints (/api/v1/ai/**): 30 req/min."
 *
 * Rate limits applied:
 *   /api/v1/ai/**         — 30 requests / minute per user (AI endpoints)
 *   /api/v1/auth/login    — 10 requests / minute per IP  (brute-force guard)
 *   All other endpoints   — 300 requests / minute per user (general limit)
 *
 * Filter chain position:
 *   JWTAuthFilter → RateLimitFilter → controller
 * Injected into SecurityConfig via:
 *   http.addFilterAfter(rateLimitFilter, JWTAuthFilter.class)
 *
 * NOTE: This class was missing from the skeleton. It lives at:
 *   infrastructure/security/RateLimitFilter.java
 * and must be injected into SecurityConfig using the real filter class name
 * JWTAuthFilter (not JwtAuthenticationFilter).
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Per-user buckets — keyed by userId string (from JWT) or IP for auth endpoints
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String bucketKey = resolveBucketKey(request);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(path));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers so Android client can implement adaptive backoff
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded: key={} path={} retryAfter={}s",
                    bucketKey, path, waitSeconds);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(waitSeconds));

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(429)
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Retry after " + waitSeconds + " seconds.")
                    .path(path)
                    .timestamp(Instant.now())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }

    /**
     * Resolve the bucket key:
     * - For AI endpoints: use userId from SecurityContext (post JWTAuthFilter)
     * - For auth/login: use IP address (user is not yet authenticated)
     * - For all others: use userId if authenticated, else IP
     */
    private String resolveBucketKey(HttpServletRequest request) {
        String path = request.getRequestURI();

        // For login endpoint — key by IP to prevent credential stuffing
        if (path.equals("/api/v1/auth/login")) {
            return "ip:" + getClientIp(request);
        }

        // For all other endpoints — key by authenticated userId if present
        var authentication = org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof com.ellh.user.entity.User user) {
            return "user:" + user.getId();
        }

        // Fall back to IP for unauthenticated requests
        return "ip:" + getClientIp(request);
    }

    /**
     * Create a Bucket4j token bucket with limits appropriate to the endpoint path.
     */
    private Bucket createBucket(String path) {
        Bandwidth limit;

        if (path.startsWith("/api/v1/ai/")) {
            // AI endpoints: 30 requests/minute (Section 4.5.5.5)
            limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
        } else if (path.equals("/api/v1/auth/login")) {
            // Login: 10 attempts/minute per IP (brute-force protection)
            limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        } else {
            // General: 300 requests/minute (generous limit for normal use)
            limit = Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)));
        }

        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
