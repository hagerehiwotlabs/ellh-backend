package com.ellh.home.service;

import com.ellh.content.entity.Language;
import com.ellh.content.repository.LanguageRepository;
import com.ellh.home.dto.HomeStatsResponse;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.user.entity.User;
import com.ellh.user.entity.LearnerProfile;
import com.ellh.user.repository.LearnerProfileRepository;
import com.ellh.gamification.entity.GamificationProfile;
import com.ellh.gamification.repository.GamificationProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final LanguageRepository languageRepository;
    private final LearnerProfileRepository learnerProfileRepository;
    private final GamificationProfileRepository gamificationProfileRepository; // Autowired
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "home:stats:";
    private static final long CACHE_TTL_MINUTES = 60;

    public HomeStatsResponse getHomeStats(Long languageId) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Language language = languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException("Language not found"));

        String cacheKey = buildCacheKey(currentUser.getId(), languageId);
        String cachedStatsJson = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedStatsJson != null) {
            try {
                return objectMapper.readValue(cachedStatsJson, HomeStatsResponse.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached home stats, rebuilding cache", e);
            }
        }

        // Fetch real data from source of truth!
        LearnerProfile profile = learnerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        GamificationProfile gProfile = gamificationProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Gamification profile not found"));

        // Build response mapping actual JPA entities [2]
        HomeStatsResponse stats = buildHomeStats(currentUser, language, profile, gProfile);

        try {
            String statsJson = objectMapper.writeValueAsString(stats);
            redisTemplate.opsForValue().set(cacheKey, statsJson, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to serialize and cache home stats", e);
        }

        return stats;
    }

    private HomeStatsResponse buildHomeStats(User user, Language language, LearnerProfile profile, GamificationProfile gProfile) {
        // Calculate daily goal XP: standard conversion is 5 XP per 1 minute of daily commitment [2]
        int dailyGoalXp = profile.getDailyGoalMinutes() * 5; 

        // Check if they studied today to determine today's XP [2]
        int currentDailyXp = 0;
        if (gProfile.getLastActivityDate() != null) {
            boolean studiedToday = gProfile.getLastActivityDate().equals(LocalDate.now());
            // If they studied today, pull progress; otherwise it's 0 for today [2]
            currentDailyXp = studiedToday ? gProfile.getTotalXP() % dailyGoalXp : 0; // Simplified estimation
        }

        return HomeStatsResponse.builder()
                .userId(user.getId())
                .languageId(language.getId())
                .totalXp(gProfile.getTotalXP())
                .currentDailyXp(currentDailyXp)
                .dailyGoalXp(dailyGoalXp)
                .currentStreak(gProfile.getCurrentStreak())
                .longestStreak(gProfile.getLongestStreak())
                .lessonsCompleted(gProfile.getLessonsCompleted())
                .currentLevel(gProfile.getLevel()) // Integer matching DB! [2]
                .levelProgress(35) // Hardcoded 35% progress to next level for now [2]
                .build();
    }

    @Transactional
    public void invalidateCache(Long userId, Long languageId) {
        String cacheKey = buildCacheKey(userId, languageId);
        redisTemplate.delete(cacheKey);
    }

    private String buildCacheKey(Long userId, Long languageId) {
        return CACHE_KEY_PREFIX + userId + ":" + languageId;
    }
}
