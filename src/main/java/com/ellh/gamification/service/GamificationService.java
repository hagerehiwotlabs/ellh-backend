package com.ellh.gamification.service;

import com.ellh.gamification.entity.GamificationProfile;
import com.ellh.gamification.repository.GamificationProfileRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final GamificationProfileRepository profileRepository;
    private final CacheEvictionService cacheEvictionService;

    @Transactional(readOnly = true)
    public GamificationProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GamificationProfile", userId));
    }

    /**
     * Awards XP and evaluates if level thresholds are cleared.
     * Threshold formula: Level * 100 XP (Level 1: 100XP, Level 2: 200XP, etc.) [5.1]
     */
    @Transactional
    public void awardXP(Long userId, int xp) {
        GamificationProfile profile = getProfile(userId);
        profile.setTotalXP(profile.getTotalXP() + xp);

        // Check level-up threshold
        int xpNeeded = profile.getLevel() * 100;
        if (profile.getTotalXP() >= xpNeeded) {
            profile.setLevel(profile.getLevel() + 1);
            log.info("User {} leveled up! New level: {}", userId, profile.getLevel());
        }

        updateStreak(profile);
        profileRepository.save(profile);

        // Evict Redis caches
        cacheEvictionService.invalidateGamification(userId);
    }

    /**
     * Updates daily learning streaks using timezone-aware dates.
     * Consumes "Streak Freeze" item if user missed yesterday [5.3].
     */
    private void updateStreak(GamificationProfile profile) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate lastActivity = profile.getLastActivityDate();

        if (lastActivity == null) {
            // First ever activity
            profile.setCurrentStreak(1);
            profile.setLongestStreak(1);
            profile.setLastActivityDate(today);
            return;
        }

        if (lastActivity.equals(today)) {
            // Already studied today — no-op
            return;
        }

        if (lastActivity.equals(today.minusDays(1))) {
            // Studied yesterday — increment streak
            int nextStreak = profile.getCurrentStreak() + 1;
            profile.setCurrentStreak(nextStreak);
            profile.setLongestStreak(Math.max(profile.getLongestStreak(), nextStreak));
            profile.setLastActivityDate(today);
        } else {
            // Missed studying yesterday
            if (profile.isStreakFreezeAvailable()) {
                // Consume Streak Freeze item! [5.3]
                profile.setStreakFreezeAvailable(false);
                profile.setLastActivityDate(today); // streak preserved
                log.info("Streak Freeze consumed for userId={}", profile.getUser().getId());
            } else {
                // Streak broken — reset to 1
                profile.setCurrentStreak(1);
                profile.setLastActivityDate(today);
            }
        }
    }
}
