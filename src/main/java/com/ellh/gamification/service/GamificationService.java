package com.ellh.gamification.service;

import com.ellh.gamification.entity.Achievement;
import com.ellh.gamification.entity.GamificationProfile;
import com.ellh.gamification.entity.UserAchievement;
import com.ellh.gamification.repository.AchievementRepository;
import com.ellh.gamification.repository.GamificationProfileRepository;
import com.ellh.gamification.repository.UserAchievementRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import com.ellh.learning.repository.UserProgressRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final GamificationProfileRepository profileRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserProgressRepository userProgressRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional(readOnly = true)
    public GamificationProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GamificationProfile", userId));
    }

    @Transactional
    public void awardXP(Long userId, int xp) {
        if (xp <= 0) return;
        
        GamificationProfile profile = getProfile(userId);
        profile.setTotalXP(profile.getTotalXP() + xp);

        // Check level-up threshold (Level * 100 XP)
        int xpNeeded = profile.getLevel() * 100;
        if (profile.getTotalXP() >= xpNeeded) {
            profile.setLevel(profile.getLevel() + 1);
            log.info("User {} leveled up! New level: {}", userId, profile.getLevel());
        }

        updateStreak(profile);
        profileRepository.save(profile);
        cacheEvictionService.invalidateGamification(userId);
    }

    private void updateStreak(GamificationProfile profile) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate lastActivity = profile.getLastActivityDate();

        if (lastActivity == null) {
            profile.setCurrentStreak(1);
            profile.setLongestStreak(1);
            profile.setLastActivityDate(today);
            return;
        }

        if (lastActivity.equals(today)) return; // Already studied today

        if (lastActivity.equals(today.minusDays(1))) {
            int nextStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(nextStreak);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), nextStreak));
            profile.setLastActivityDate(today);
        } else {
            if (profile.isStreakFreezeAvailable()) {
                profile.setStreakFreezeAvailable(false);
                profile.setLastActivityDate(today);
                log.info("Streak Freeze consumed for userId={}", profile.getUser().getId());
            } else {
                profile.setCurrentStreak(1);
                profile.setLastActivityDate(today);
            }
        }
    }

    /**
     * EVALUATOR ENGINE: Parses JSONB criteria and tests against live DB state.
     * Returns a list of newly unlocked Achievement IDs to trigger Android Lottie Animations.
     */
    @Transactional
    public List<Long> checkAndAwardAchievements(Long userId) {
        GamificationProfile profile = getProfile(userId);
        List<Achievement> activeAchievements = achievementRepository.findByActiveTrue();
        List<Long> newlyUnlockedIds = new ArrayList<>();

        for (Achievement achievement : activeAchievements) {
            // Skip if already earned
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
                continue;
            }

            if (evaluateAchievementCriteria(userId, profile, achievement)) {
                // 1. Save the Badge
                UserAchievement ua = UserAchievement.builder()
                        .user(profile.getUser())
                        .achievement(achievement)
                        .completed(true)
                        .build();
                userAchievementRepository.save(ua);
                
                // 2. Grant the Economic Reward (Triggers cascading Level-Up checks)
                if (achievement.getXpReward() > 0) {
                    awardXP(userId, achievement.getXpReward());
                }
                
                newlyUnlockedIds.add(achievement.getId());
                log.info("User {} unlocked achievement: {} (+{} XP)", userId, achievement.getName(), achievement.getXpReward());
            }
        }
        
        if (!newlyUnlockedIds.isEmpty()) {
            cacheEvictionService.invalidateGamification(userId);
        }
        
        return newlyUnlockedIds;
    }

    private boolean evaluateAchievementCriteria(Long userId, GamificationProfile profile, Achievement achievement) {
        Map<String, Object> criteria = achievement.getCriteriaValue();
        if (criteria == null) return false;

        try {
            switch (achievement.getCriteriaType()) {
                case "STREAK_DAYS":
                    int reqStreak = ((Number) criteria.get("count")).intValue();
                    return profile.getCurrentStreak() >= reqStreak;

                case "XP_TOTAL":
                    int reqXp = ((Number) criteria.get("threshold")).intValue();
                    return profile.getTotalXP() >= reqXp;

                case "LESSON_COMPLETE":
                    int reqLessons = ((Number) criteria.get("count")).intValue();
                    long completed = userProgressRepository.countCompletedLessonsForUser(userId);
                    return completed >= reqLessons;

                case "EXERCISE_SCORE":
                    int reqScore = ((Number) criteria.get("score")).intValue();
                    int maxScore = userProgressRepository.findMaxExerciseScoreForUser(userId);
                    return maxScore >= reqScore;

                default:
                    log.warn("Unknown achievement criteria_type: {}", achievement.getCriteriaType());
                    return false;
            }
        } catch (Exception e) {
            log.error("Failed to parse JSONB criteria for achievement {}", achievement.getId(), e);
            return false;
        }
    }
}
