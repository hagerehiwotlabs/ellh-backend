package com.ellh.home.service;

import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.home.dto.HomeStatsResponse;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * HomeService — aggregates user engagement statistics for the home screen.
 *
 * Responsibilities:
 *  - Calculate daily XP (XP earned today)
 *  - Fetch current streak from cache
 *  - Calculate current level and progress
 *  - Return aggregated HomeStatsResponse
 *
 * Caching:
 *  - HomeStats cached in Redis with 1-hour TTL (frequent updates during active sessions)
 *  - Cache key: home:stats:{userId}:{languageId}
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final LanguageRepository languageRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "home:stats:";
    private static final long CACHE_TTL_MINUTES = 60;

    /**
     * Get home stats for the authenticated user for a specific language.
     *
     * @param languageId The target language ID
     * @return HomeStatsResponse with aggregated metrics
     * @throws ResourceNotFoundException if language not found
     */
    public HomeStatsResponse getHomeStats(Long languageId) {
        // Get authenticated user
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        // Validate language exists
        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Language not found with id: " + languageId));

        // Check cache
        String cacheKey = buildCacheKey(currentUser.getId(), languageId);
        String cachedStatsJson = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedStatsJson != null) {
            try {
                HomeStatsResponse cachedStats = objectMapper.readValue(cachedStatsJson, HomeStatsResponse.class);
                log.debug("Cache hit for home stats: userId={}, languageId={}", 
                        currentUser.getId(), languageId);
                return cachedStats;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached home stats, rebuilding cache", e);
            }
        }

        log.debug("Cache miss for home stats: userId={}, languageId={}", 
                currentUser.getId(), languageId);

        // Build response from source of truth (to be implemented with real data)
        HomeStatsResponse stats = buildHomeStats(currentUser, language);

        // Cache the result
        try {
            String statsJson = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(cacheKey, statsJson, 
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to serialize and cache home stats", e);
        }

        return stats;
    }

    /**
     * Build HomeStatsResponse from user data.
     * This is a foundation method that should be enriched with actual
     * data from UserProgress, StreakTracker, and XP tables.
     */
    private HomeStatsResponse buildHomeStats(User user, Language language) {
        return HomeStatsResponse.builder()
                .userId(user.getId())
                .languageId(language.getId())
                .totalXp(0)              // TODO: Fetch from user_progress
                .dailyXp(0)              // TODO: Calculate from today's activity
                .currentStreak(0)        // TODO: Fetch from streak_tracker or cache
                .longestStreak(0)        // TODO: Fetch from user_stats
                .lessonsCompleted(0)     // TODO: Count from lesson_progress
                .exercisesCompleted(0)   // TODO: Count from exercise_results
                .currentLevel("Beginner") // TODO: Calculate from totalXp
                .levelProgress(0)        // TODO: Calculate XP% to next level
                .build();
    }

    /**
     * Invalidate home stats cache when user progress changes.
     * Should be called after lesson completion, exercise submission, etc.
     */
    @Transactional
    public void invalidateCache(Long userId, Long languageId) {
        String cacheKey = buildCacheKey(userId, languageId);
        redisTemplate.delete(cacheKey);
        log.debug("Invalidated home stats cache: userId={}, languageId={}", 
                userId, languageId);
    }

    private String buildCacheKey(Long userId, Long languageId) {
        return CACHE_KEY_PREFIX + userId + ":" + languageId;
    }
}
