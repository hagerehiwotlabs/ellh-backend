package com.ellh.gamification.service;

import com.ellh.gamification.model.Achievement;
import com.ellh.gamification.model.GamificationProfile;
import com.ellh.gamification.model.UserAchievement;
import com.ellh.gamification.repository.GamificationProfileRepository;
import com.ellh.gamification.repository.AchievementRepository;
import com.ellh.gamification.repository.UserAchievementRepository;
import com.ellh.infrastructure.notification.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GamificationService — coverage gap-fill (Sprint 9 NFR-15).
 *
 * Tests cover:
 *   1.  XP award updates totalXP and level correctly.
 *   2.  Level threshold: L1=0, L2=100, L3=250 (XP table from spec).
 *   3.  Streak increment: completing on day N+1 increments currentStreak.
 *   4.  Streak break: skipping a day resets currentStreak to 1.
 *   5.  Streak maintains longestStreak correctly.
 *   6.  Achievement unlock: LESSON_COMPLETE criteria evaluates correctly.
 *   7.  Achievement unlock: STREAK_DAYS criteria evaluates correctly.
 *   8.  Achievement unlock: XP_TOTAL criteria evaluates correctly.
 *   9.  Duplicate achievement prevention: UserAchievement not inserted twice.
 *  10.  checkAchievements returns list of newly unlocked achievements.
 *
 * Section 4.5.1 GamificationService.
 * NFR-15 — ≥70% unit test coverage on service classes.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock private GamificationProfileRepository profileRepository;
    @Mock private AchievementRepository         achievementRepository;
    @Mock private UserAchievementRepository     userAchievementRepository;
    @Mock private NotificationService           notificationService;

    @InjectMocks
    private GamificationService gamificationService;

    private static final Long USER_ID = 1L;

    private GamificationProfile defaultProfile() {
        GamificationProfile p = new GamificationProfile();
        p.setUserId(USER_ID);
        p.setTotalXP(0);
        p.setCurrentStreak(0);
        p.setLongestStreak(0);
        p.setLevel(1);
        p.setLastActivityDate(null);
        return p;
    }

    @BeforeEach
    void setUp() {
        when(profileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(defaultProfile()));
    }

    @Test
    void awardXP_updatesTotalXP() {
        gamificationService.awardXP(USER_ID, 25, "LESSON_COMPLETE");
        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(25, captor.getValue().getTotalXP());
    }

    @Test
    void awardXP_level2CalculatedAt100XP() {
        GamificationProfile p = defaultProfile();
        p.setTotalXP(90);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(p));

        gamificationService.awardXP(USER_ID, 10, "LESSON_COMPLETE");

        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getLevel());
        assertEquals(100, captor.getValue().getTotalXP());
    }

    @Test
    void awardXP_level3CalculatedAt250XP() {
        GamificationProfile p = defaultProfile();
        p.setTotalXP(240);
        p.setLevel(2);
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(p));

        gamificationService.awardXP(USER_ID, 10, "LESSON_COMPLETE");

        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getLevel());
    }

    @Test
    void awardXP_streakIncrements_whenActivityOnConsecutiveDay() {
        GamificationProfile p = defaultProfile();
        p.setCurrentStreak(3);
        p.setLastActivityDate(LocalDate.now().minusDays(1)); // yesterday
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(p));

        gamificationService.awardXP(USER_ID, 10, "EXERCISE_COMPLETE");

        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getCurrentStreak());
    }

    @Test
    void awardXP_streakResets_whenDayIsSkipped() {
        GamificationProfile p = defaultProfile();
        p.setCurrentStreak(5);
        p.setLastActivityDate(LocalDate.now().minusDays(2)); // two days ago
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(p));

        gamificationService.awardXP(USER_ID, 10, "EXERCISE_COMPLETE");

        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getCurrentStreak()); // reset to 1
    }

    @Test
    void awardXP_longestStreak_updatedWhenCurrentExceedsPrevious() {
        GamificationProfile p = defaultProfile();
        p.setCurrentStreak(7);
        p.setLongestStreak(7);
        p.setLastActivityDate(LocalDate.now().minusDays(1));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(p));

        gamificationService.awardXP(USER_ID, 10, "EXERCISE_COMPLETE");

        ArgumentCaptor<GamificationProfile> captor =
                ArgumentCaptor.forClass(GamificationProfile.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(8, captor.getValue().getCurrentStreak());
        assertEquals(8, captor.getValue().getLongestStreak());
    }

    @Test
    void checkAchievements_lessonCompleteCriteria_unlocksWhenThresholdMet() {
        Achievement a = buildAchievement(1L, "LESSON_COMPLETE", 1);
        when(achievementRepository.findAllActive()).thenReturn(Arrays.asList(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(USER_ID, 1L))
                .thenReturn(false);

        GamificationProfile p = defaultProfile();
        p.setTotalXP(50); // doesn't matter for LESSON_COMPLETE
        // Simulate lessonsCompleted passed as parameter
        java.util.List<Long> unlocked =
                gamificationService.checkAchievements(USER_ID, 1, 0, 50, 0);

        assertFalse(unlocked.isEmpty(), "LESSON_COMPLETE achievement should be unlocked");
        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    @Test
    void checkAchievements_duplicatePrevention_doesNotUnlockTwice() {
        Achievement a = buildAchievement(1L, "LESSON_COMPLETE", 1);
        when(achievementRepository.findAllActive()).thenReturn(Arrays.asList(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(USER_ID, 1L))
                .thenReturn(true); // already earned

        java.util.List<Long> unlocked =
                gamificationService.checkAchievements(USER_ID, 1, 0, 50, 0);

        assertTrue(unlocked.isEmpty(), "Already earned achievement must not unlock again");
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void checkAchievements_xpTotalCriteria_unlocksAtThreshold() {
        Achievement a = buildAchievement(2L, "XP_TOTAL", 100);
        when(achievementRepository.findAllActive()).thenReturn(Arrays.asList(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(USER_ID, 2L))
                .thenReturn(false);

        java.util.List<Long> unlocked =
                gamificationService.checkAchievements(USER_ID, 0, 0, 100, 0);

        assertFalse(unlocked.isEmpty(), "XP_TOTAL achievement should unlock at 100 XP");
    }

    @Test
    void checkAchievements_streakDaysCriteria_unlocksAtThreshold() {
        Achievement a = buildAchievement(3L, "STREAK_DAYS", 7);
        when(achievementRepository.findAllActive()).thenReturn(Arrays.asList(a));
        when(userAchievementRepository.existsByUserIdAndAchievementId(USER_ID, 3L))
                .thenReturn(false);

        java.util.List<Long> unlocked =
                gamificationService.checkAchievements(USER_ID, 0, 7, 0, 0);

        assertFalse(unlocked.isEmpty(), "STREAK_DAYS achievement should unlock at 7 days");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Achievement buildAchievement(Long id, String criteriaType, int criteriaValue) {
        Achievement a = new Achievement();
        a.setAchievementId(id);
        a.setCriteriaType(criteriaType);
        a.setCriteriaValue(String.valueOf(criteriaValue));
        a.setXpReward(50);
        a.setActive(true);
        a.setName("Test Achievement " + id);
        return a;
    }
}
