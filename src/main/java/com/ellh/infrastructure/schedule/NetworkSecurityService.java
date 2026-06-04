package com.ellh.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP security filter enforcing TLS and adding security headers.
 *
 * Applied as the first filter in the chain (@Order(1)).
 * Enforces:
 *   - HTTP Strict Transport Security (HSTS) header on all responses.
 *   - Rejection of non-HTTPS requests in production (detected via X-Forwarded-Proto).
 *   - Security headers per OWASP recommendations.
 *
 * On Render.com, TLS is terminated at the load balancer. Requests arrive
 * to Spring Boot over plain HTTP with X-Forwarded-Proto: https header.
 * This filter uses that header to detect the original protocol.
 *
 * Section 4.5.5.5 — Security enforcement point 1 (TLS 1.3).
 * NFR-12 — "All external communication uses TLS 1.2 or higher;
 *            plain HTTP requests are rejected at the API gateway."
 */
@Component
@Order(1)
public class NetworkSecurityService extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NetworkSecurityService.class);

    /** Health check endpoint is exempt from HTTPS redirect (Render.com pings via HTTP). */
    private static final String HEALTH_PATH = "/actuator/health";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String proto    = request.getHeader("X-Forwarded-Proto");
        String path     = request.getRequestURI();

        // ── TLS enforcement (NFR-12) ───────────────────────────────────────
        // In production: X-Forwarded-Proto=http means original request was HTTP.
        // Redirect to HTTPS. Health check is exempt (Render.com pings over HTTP).
        if ("http".equalsIgnoreCase(proto) && !HEALTH_PATH.equals(path)) {
            String httpsUrl = "https://"
                    + request.getServerName()
                    + request.getRequestURI()
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            log.warn("NetworkSecurityService: HTTP request detected, redirecting to HTTPS: {}",
                    path);
            response.sendRedirect(httpsUrl);
            return;
        }

        // ── Security headers (OWASP) ──────────────────────────────────────
        // HSTS: 1 year, includeSubDomains — enforces HTTPS on all subdomains
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

        // X-Content-Type-Options: prevents MIME sniffing attacks
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options: prevents clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Content-Security-Policy: restrict resource loading
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; connect-src 'self'");

        // Referrer-Policy: no referrer information leaked
        response.setHeader("Referrer-Policy", "no-referrer");

        // Cache-Control: prevent sensitive API responses from being cached by proxies
        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/users")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }
}
