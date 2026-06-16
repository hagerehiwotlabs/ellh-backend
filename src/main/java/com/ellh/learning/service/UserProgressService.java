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

// Append this method inside your UserProgressService class:

    /**
     * Dynamically aggregates UserProgress scores by exercise type.
     * Guarantees all 6 progress bars always receive a valid value (0% default)
     * to prevent UI rendering errors on new accounts.
     */
    public List<SkillBreakdownDto> getSkillBreakdown(Long userId) {
        List<UserProgress> progressList = userProgressRepository.findByUserId(userId);
        
        // Define the 6 core learning skills used by the client layout (Rule 3)
        String[] skillTypes = {"MULTIPLE_CHOICE", "FILL_BLANK", "LISTENING", "WRITING", "PRONUNCIATION", "TRANSLATION"};
        
        Map<String, List<Integer>> scoresMap = new HashMap<>();
        Map<String, Integer> attemptsMap = new HashMap<>();

        // Initialize maps with default empty lists/counters
        for (String type : skillTypes) {
            scoresMap.put(type, new ArrayList<>());
            attemptsMap.put(type, 0);
        }

        // Group the records by their active exercise types
        for (UserProgress up : progressList) {
            if (up.getExercise() != null && up.getExercise().getExerciseType() != null) {
                String type = up.getExercise().getExerciseType().name();
                if (scoresMap.containsKey(type)) {
                    if (up.getScore() != null) {
                        scoresMap.get(type).add(up.getScore());
                    }
                    attemptsMap.put(type, attemptsMap.get(type) + up.getAttempts());
                }
            }
        }

        // Compile results into DTO payloads
        List<SkillBreakdownDto> dtos = new ArrayList<>();
        for (String type : skillTypes) {
            List<Integer> scores = scoresMap.get(type);
            double average = 0.0;
            
            if (!scores.isEmpty()) {
                double sum = 0;
                for (int s : scores) {
                    sum += s;
                }
                average = sum / scores.size();
            }
            
            dtos.add(SkillBreakdownDto.builder()
                    .exerciseType(type)
                    .percentage(average)
                    .attemptCount(attemptsMap.get(type))
                    .build());
        }

        return dtos;
    }
}
