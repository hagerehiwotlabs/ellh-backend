package com.ellh.infrastructure.cache;

/**
 * Centralised Redis key constants.
 * All cache keys follow the pattern:  prefix:{id}
 * Changing a prefix here invalidates all existing cache entries for that type.
 *
 * TTLs are NOT defined here — they are configured per-service where the
 * cache is written (single responsibility for write + TTL together).
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {} // utility class

    // Session tokens — 24hr TTL (aligned to JWT access token expiry)
    public static final String SESSION_PREFIX    = "session:";

    // Lesson content — 7-day TTL (Section 4.5.4.4 Redis TTLs)
    public static final String LESSON_PREFIX     = "lesson:";

    // Language list — 7-day TTL (rarely changes)
    public static final String LANGUAGES_KEY     = "languages:all";

    // Translation results — 30-day TTL (Section 4.5.4.4)
    // Key format: translate:{srcLang}:{tgtLang}:{sha256(text)}
    public static final String TRANSLATE_PREFIX  = "translate:";

    // Gamification profile — 1hr TTL
    public static final String GAMIFICATION_PREFIX = "gamification:";

    // Progress summary — 24hr TTL
    public static final String PROGRESS_PREFIX   = "progress:";

    // Idempotency keys for sync batch processing — 48hr TTL
    public static final String IDEMPOTENCY_PREFIX = "idempotency:";

    public static String sessionKey(Long userId)         { return SESSION_PREFIX + userId; }
    public static String lessonKey(Long lessonId)        { return LESSON_PREFIX + lessonId; }
    public static String gamificationKey(Long userId)    { return GAMIFICATION_PREFIX + userId; }
    public static String progressKey(Long userId)        { return PROGRESS_PREFIX + userId; }
    public static String idempotencyKey(String key)      { return IDEMPOTENCY_PREFIX + key; }
}
