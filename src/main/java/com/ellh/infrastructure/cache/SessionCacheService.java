package com.ellh.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Manages user session data in Redis.
 * Section 4.5.1 — Infrastructure Domain; Section 4.5.4.4 — Redis TTLs.
 *
 * Session cache stores the JWT access token (or a session marker) keyed
 * by userId with a 24-hour TTL — aligned to the JWT access token expiry.
 *
 * Used by:
 *   - AuthService: writes session on login, deletes on logout / account deletion
 *   - JWTAuthFilter: (optionally) validates session is still active
 *   - GDPR EC-08 Step 6: clearSession() called during account deletion
 *
 * NOTE: This class was missing from the skeleton. It lives at:
 *   infrastructure/cache/SessionCacheService.java
 * alongside CacheEvictionService.java as specified in the Sprint 1 additions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCacheService {

    /** TTL aligned to JWT access token expiry (Section 4.5.4.4). */
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Stores a session entry for the user.
     * Called after successful login or token refresh.
     *
     * @param userId    the authenticated user's database ID
     * @param tokenHash a non-sensitive identifier (e.g. first 8 chars of JWT)
     *                  stored as the value for session presence checks
     */
    public void createSession(Long userId, String tokenHash) {
        String key = CacheKeyConstants.sessionKey(userId);
        redisTemplate.opsForValue().set(key, tokenHash, SESSION_TTL);
        log.debug("Session created for userId={}", userId);
    }

    /**
     * Returns the session token hash if the session is active.
     * Returns empty if the session has expired or was explicitly cleared.
     */
    public Optional<String> getSession(Long userId) {
        String value = redisTemplate.opsForValue().get(CacheKeyConstants.sessionKey(userId));
        return Optional.ofNullable(value);
    }

    /**
     * Returns true if a session entry exists for this userId.
     * Used for lightweight session validity checks without full JWT re-validation.
     */
    public boolean hasActiveSession(Long userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(CacheKeyConstants.sessionKey(userId)));
    }

    /**
     * Extends the session TTL by another 24 hours.
     * Called on every successful authenticated request to implement sliding expiry.
     */
    public void extendSession(Long userId) {
        redisTemplate.expire(CacheKeyConstants.sessionKey(userId), SESSION_TTL);
    }

    /**
     * Deletes the session entry immediately.
     * Called on:
     *   - POST /api/v1/auth/logout
     *   - GDPR EC-08 account deletion (Step 6)
     *   - User account suspension by SystemAdmin
     */
    public void clearSession(Long userId) {
        redisTemplate.delete(CacheKeyConstants.sessionKey(userId));
        log.info("Session cleared for userId={}", userId);
    }
}
