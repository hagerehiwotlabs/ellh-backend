package com.ellh.sync.service;

import com.ellh.content.entity.Exercise;
import com.ellh.content.entity.Lesson;
import com.ellh.content.repository.ExerciseRepository;
import com.ellh.content.repository.LessonRepository;
import com.ellh.feedback.entity.FeedbackReport;
import com.ellh.feedback.entity.FeedbackSeverity;
import com.ellh.feedback.entity.FeedbackStatus;
import com.ellh.feedback.entity.TargetType;
import com.ellh.feedback.repository.FeedbackReportRepository;
import com.ellh.infrastructure.cache.CacheKeyConstants;
import com.ellh.learning.entity.UserProgress;
import com.ellh.learning.repository.UserProgressRepository;
import com.ellh.sync.dto.SyncBatchRequest;
import com.ellh.sync.dto.SyncBatchResponse;
import com.ellh.user.entity.User;
import com.ellh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final FeedbackReportRepository feedbackRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final com.ellh.sync.service.ConflictResolver conflictResolver;

    @Transactional
    public SyncBatchResponse processBatch(SyncBatchRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        int processed = 0;
        int skipped = 0;
        List<Long> achievements = new ArrayList<>(); // placeholder for Sprint 8 metrics

        for (SyncBatchRequest.SyncEventDto event : request.getEvents()) {
            String redisKey = CacheKeyConstants.idempotencyKey(event.getIdempotencyKey());
            
            // Check Redis (48hr TTL) for duplicate transaction protection (SYNC-01)
            Boolean isDuplicate = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(isDuplicate)) {
                skipped++;
                continue;
            }

            try {
                processEvent(currentUser, event);
                
                // Cache key in Redis with 48-hour expiration
                redisTemplate.opsForValue().set(redisKey, "PROCESSED", Duration.ofHours(48));
                processed++;
            } catch (Exception e) {
                log.error("Sync failure for event {}: {}", event.getIdempotencyKey(), e.getMessage());
            }
        }

        return SyncBatchResponse.builder()
                .processedCount(processed)
                .skippedCount(skipped)
                .conflictCount(0)
                .achievementsUnlocked(achievements)
                .build();
    }

    private void processEvent(User user, SyncBatchRequest.SyncEventDto event) {
        Map<String, Object> payload = event.getPayload();
        String action = event.getActionType();

        // Safe timestamp conversion from the incoming sync event [7.1]
        Instant eventTimestamp = event.getCreatedAt() > 0 
                ? Instant.ofEpochMilli(event.getCreatedAt()) 
                : Instant.now();

        if ("EXERCISE_COMPLETE".equals(action)) {
            Long lessonId = ((Number) payload.get("lessonId")).longValue();
            Long exerciseId = ((Number) payload.get("exerciseId")).longValue();
            String status = (String) payload.get("status");
            Integer score = ((Number) payload.get("score")).intValue();

            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow();

            // Build the proposed incoming progress state
            UserProgress incoming = UserProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .exercise(exercise)
                    .status(status)
                    .score(score)
                    .attempts(1) // Base count for this attempt
                    .lastAttemptAt(eventTimestamp)
                    .completedAt("COMPLETED".equals(status) ? eventTimestamp : null)
                    .build();

            Optional<UserProgress> existingOpt = userProgressRepository.findProgress(user.getId(), lessonId, exerciseId);

            if (existingOpt.isPresent()) {
                UserProgress existing = existingOpt.get();
                
                // Invoke ConflictResolver to handle multi-device state conflicts [7.1]
                UserProgress resolved = conflictResolver.resolve(incoming, existing);

                existing.setStatus(resolved.getStatus());
                existing.setScore(resolved.getScore());
                existing.setAttempts(existing.getAttempts() + 1); // Increment actual attempts count
                existing.setLastAttemptAt(resolved.getLastAttemptAt());
                existing.setCompletedAt(resolved.getCompletedAt());
                
                userProgressRepository.save(existing);
            } else {
                userProgressRepository.save(incoming);
            }

        } else if ("LESSON_COMPLETE".equals(action)) {
            Long lessonId = ((Number) payload.get("lessonId")).longValue();
            // Safeguard to read actual quiz final score percentage if passed from Android client [4.7, 3.7]
            Integer score = payload.containsKey("score") ? ((Number) payload.get("score")).intValue() : 100;
            
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

            // Build the proposed incoming lesson completion state
            UserProgress incoming = UserProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .exercise(null)
                    .status("COMPLETED")
                    .score(score)
                    .attempts(1)
                    .lastAttemptAt(eventTimestamp)
                    .completedAt(eventTimestamp)
                    .build();

            Optional<UserProgress> existingOpt = userProgressRepository.findProgress(user.getId(), lessonId, null);

            if (existingOpt.isPresent()) {
                UserProgress existing = existingOpt.get();
                
                // Conflict resolution evaluation for lesson state [7.1]
                UserProgress resolved = conflictResolver.resolve(incoming, existing);

                existing.setStatus(resolved.getStatus());
                existing.setScore(resolved.getScore());
                existing.setAttempts(existing.getAttempts() + 1);
                existing.setLastAttemptAt(resolved.getLastAttemptAt());
                existing.setCompletedAt(resolved.getCompletedAt());
                
                userProgressRepository.save(existing);
            } else {
                userProgressRepository.save(incoming);
            }

        } else if ("FEEDBACK_SUBMIT".equals(action)) {
            FeedbackReport report = FeedbackReport.builder()
                    .user(user)
                    .targetType(TargetType.valueOf((String) payload.get("targetType")))
                    .targetId((String) payload.get("targetId"))
                    .issueType((String) payload.get("issueType"))
                    .description((String) payload.get("description"))
                    .status(FeedbackStatus.OPEN)
                    .severity("HIGH".equals(payload.get("severity")) ? FeedbackSeverity.HIGH : FeedbackSeverity.LOW)
                    .build();
            feedbackRepository.save(report);
        }
    }
}
