package com.ellh.learning.service;

import com.ellh.content.entity.Lesson;
import com.ellh.content.repository.LessonRepository;
import com.ellh.infrastructure.cache.CacheEvictionService;
import com.ellh.learning.entity.LearnerLanguage;
import com.ellh.learning.entity.UserProgress;
import com.ellh.learning.repository.LearnerLanguageRepository;
import com.ellh.learning.repository.UserProgressRepository;
import com.ellh.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import com.ellh.learning.dto.SkillBreakdownDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProgressService {

    private final UserProgressRepository userProgressRepository;
    private final LearnerLanguageRepository learnerLanguageRepository;
    private final LessonRepository lessonRepository;
    private final CacheEvictionService cacheEvictionService;

    /**
     * Triggered on LESSON_COMPLETE sync events.
     * Computes the new overall mastery percentage for this language.
     * Mastery % = (Completed Lessons * 100) / Total Lessons in active language.
     */
    @Transactional
    public void recalculateLanguageMastery(User user, Lesson completedLesson) {
        Long languageId = completedLesson.getLanguage().getId();

        // 1. Find the LearnerLanguage profile
        List<LearnerLanguage> learnerLanguages = learnerLanguageRepository.findAll(); // simplified filter
        LearnerLanguage targetProfile = null;
        for (LearnerLanguage ll : learnerLanguages) {
            if (ll.getUser().getId().equals(user.getId()) && ll.getLanguage().getId().equals(languageId)) {
                targetProfile = ll;
                break;
            }
        }

        if (targetProfile == null) return;

        // 2. Count total active lessons for this language
        long totalLessons = lessonRepository.countByLanguageIdAndActiveTrue(languageId);
        if (totalLessons == 0) return;

        // 3. Count completed lessons
        long completedCount = 0; // standard count from userProgress database checks
        // userProgressRepository.countCompletedLessonsForUser(user.getId(), languageId)

        // For simulation, we increment their count
        int updatedCompleted = targetProfile.getLessonsCompleted() + 1;
        targetProfile.setLessonsCompleted(updatedCompleted);

        // 4. Calculate final mastery percentage
        double mastery = (double) (updatedCompleted * 100L) / totalLessons;
        targetProfile.setMasteryPercent(BigDecimal.valueOf(Math.min(100.0, mastery)));

        learnerLanguageRepository.save(targetProfile);

        // 5. Evict Redis dashboard caches to force immediate updates on client reload
        cacheEvictionService.invalidateProgress(user.getId());
        cacheEvictionService.invalidateGamification(user.getId());

        log.info("Mastery recalculated: userId={} languageId={} newMastery={}%%",
                user.getId(), languageId, targetProfile.getMasteryPercent());
    }

    public List<SkillBreakdownDto> getSkillBreakdown(Long userId) {
        // O(1) Database Query: Let PostgreSQL do the heavy lifting, not the JVM!
        List<Object[]> aggregatedData = userProgressRepository.getAggregatedSkillBreakdown(userId);
        
        String[] coreSkills = {"MULTIPLE_CHOICE", "FILL_BLANK", "LISTENING", "WRITING", "PRONUNCIATION", "TRANSLATION"};
        Map<String, SkillBreakdownDto> dtoMap = new java.util.HashMap<>();

        // Initialize all skills to 0% to prevent UI rendering bugs on new accounts
        for (String skill : coreSkills) {
            dtoMap.put(skill, SkillBreakdownDto.builder().exerciseType(skill).percentage(0.0).attemptCount(0).build());
        }

        // Map the PostgreSQL results directly to the DTOs
        for (Object[] row : aggregatedData) {
            String type = ((com.ellh.content.entity.ExerciseType) row[0]).name();
            double avgScore = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            int attempts = row[2] != null ? ((Number) row[2]).intValue() : 0;

            if (dtoMap.containsKey(type)) {
                dtoMap.get(type).setPercentage(avgScore);
                dtoMap.get(type).setAttemptCount(attempts);
            }
        }

        return new java.util.ArrayList<>(dtoMap.values());
    }
}
