package com.ellh.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Generates and validates JWT access and refresh tokens.
 * Section 4.5.1 — Infrastructure Domain; Section 4.5.5.5 — Security Layer.
 *
 * Access token: 24-hour expiry, HMAC-SHA256, contains userId and userType claims.
 * Refresh token: 30-day expiry, HMAC-SHA256, contains only userId claim.
 *
 * JWT_SECRET must be at least 32 characters (256 bits) for HMAC-SHA256.
 * It is injected from the JWT_SECRET environment variable — never hard-coded.
 */
@Slf4j
@Service
public class JWTService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryHours;
    private final long refreshTokenExpiryDays;

    public JWTService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-hours:24}") long accessTokenExpiryHours,
            @Value("${jwt.refresh-token-expiry-days:30}") long refreshTokenExpiryDays) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters for HMAC-SHA256. " +
                    "Set the JWT_SECRET environment variable.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryHours = accessTokenExpiryHours;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Mints a 24-hour access token containing the userId and userType claims.
     * Used by AuthController on login and token refresh.
     */
    public String generateAccessToken(Long userId, String email, String userType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)                 // ← use email, not userId
                .claim("userId", userId)        // still include userId as custom claim
                .claim("userType", userType)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiryHours, ChronoUnit.HOURS)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Mints a 30-day refresh token. Contains only userId — no role claim.
     * Stored encrypted on the Android device via EncryptedSharedPreferences.
     */
    public String generateRefreshToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS)))
                .signWith(signingKey)
                .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /** Returns true if the token signature is valid and it has not expired. */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractUsername(token);  // subject = email
            return email.equals(userDetails.getUsername()) // user email (e.g., "ab@gmail.com")
                    && !isTokenExpired(token);
        } catch (JwtException | ClassCastException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /** Validates a refresh token — only checks signature and expiry. */
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "REFRESH".equals(claims.get("tokenType", String.class))
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserType(String token) {
        return extractAllClaims(token).get("userType", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserIdFromClaims(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }
}
