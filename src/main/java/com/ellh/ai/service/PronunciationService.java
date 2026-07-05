package com.ellh.ai.service;

import com.ellh.ai.entity.PronunciationAttempt;
import com.ellh.ai.repository.PronunciationAttemptRepository;
import com.ellh.content.entity.Exercise;
import com.ellh.content.repository.ExerciseRepository;
import com.ellh.infrastructure.storage.StorageService;
import com.ellh.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationService {

    private final PronunciationAttemptRepository pronunciationRepository;
    private final ExerciseRepository exerciseRepository;
    private final com.ellh.ai.gateway.AIServiceGateway aiServiceGateway;
    private final StorageService storageService;

    @Transactional
    public com.ellh.ai.dto.PronunciationResponse scorePronunciation(
            MultipartFile audio,
            Long exerciseId,
            User user,
            String languageCode,
            String targetWord,
            String audioHash) {

        long startTime = System.currentTimeMillis();

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found"));

        // 1. Physically Upload the Audio to Cloud Storage
        String persistentAudioUrl = null;
        try {
            persistentAudioUrl = storageService.uploadAudio(audio, audioHash);
        } catch (Exception e) {
            log.error("Storage outage: Failed to persist audio {}. Proceeding with ephemeral AI scoring.", audioHash, e);
            // Graceful degradation: We don't crash if S3 is down; we just lose the audit capability for this record.
            persistentAudioUrl = "unreachable_storage_bucket"; 
        }

        // 2. Score via the Outbound AI Gateway (Circuit Breaker managed)
        com.ellh.ai.dto.PronunciationResponse response;
        try {
            response = aiServiceGateway.analyzePronunciation(audio, user, targetWord, audioHash);
        } catch (Exception e) {
            log.error("AI Scorer failed. Circuit breaker tripped. Throwing exception.", e);
            throw new com.ellh.infrastructure.exception.AIServiceUnavailableException("Speech engines unavailable.");
        }

        int elapsed = (int) (System.currentTimeMillis() - startTime);

        // 3. Persist the Attempt with the REAL Cloud URL
        PronunciationAttempt attempt = PronunciationAttempt.builder()
                .user(user)
                .exercise(exercise)
                .audioUrl(persistentAudioUrl)
                .confidenceScore(BigDecimal.valueOf(response.getConfidenceScore()))
                .feedbackText(response.getFeedbackText())
                .processingTimeMs(elapsed)
                .build();
        pronunciationRepository.save(attempt);

        response.setProcessingTimeMs(elapsed);
        return response;
    }
}
