package com.ellh.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Handles explicit cache invalidation for content updates.
 * Section 4.5.1 — Infrastructure Domain (CacheEvictionService).
 *
 * Called by LessonService and ContentUpdateService whenever a ContentAdmin
 * updates lesson content. Without eviction, learners would receive stale
 * lesson content from Redis until the 7-day TTL expires naturally.
 *
 * Section 4.5.2.6 — Cross-Store Consistency: cache eviction is triggered
 * post-commit after any lesson or contrastive-rule update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Deletes the cached lesson content for a given lessonId.
     * Must be called AFTER the database transaction commits — not inside it.
     *
     * @param lessonId the PostgreSQL lesson ID (not the MongoDB ObjectId)
     */
    public void invalidateLesson(Long lessonId) {
        String key = CacheKeyConstants.lessonKey(lessonId);
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Cache evicted: lessonId={}", lessonId);
        } else {
            log.debug("Cache eviction no-op (key not present): lessonId={}", lessonId);
        }
    }

    /**
     * Deletes the cached language list.
     * Called when a new language is added or an existing one is deactivated.
     */
    public void invalidateLanguages() {
        redisTemplate.delete(CacheKeyConstants.LANGUAGES_KEY);
        log.info("Language list cache evicted");
    }

    /**
     * Deletes all cached entries for a specific user's gamification state.
     * Called by GamificationService after XP award or achievement unlock.
     */
    public void invalidateGamification(Long userId) {
        redisTemplate.delete(CacheKeyConstants.gamificationKey(userId));
    }

    /**
     * Deletes all cached progress entries for a specific user.
     * Called by UserProgressService after progress update.
     */
    public void invalidateProgress(Long userId) {
        redisTemplate.delete(CacheKeyConstants.progressKey(userId));
    }
}
