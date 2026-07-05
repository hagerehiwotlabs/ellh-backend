package com.ellh.infrastructure.schedule;

import com.ellh.user.repository.UserRepository;
import com.ellh.content.repository.ContentUpdateLogDocumentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GdprCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(GdprCleanupJob.class);

    private final UserRepository userRepository;
    private final ContentUpdateLogDocumentRepository auditLogRepository;

    public GdprCleanupJob(UserRepository userRepository, ContentUpdateLogDocumentRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void runDailyGdprPurge() {
        log.info("GdprCleanupJob: starting daily GDPR purge at {}", LocalDateTime.now());

        // Step A: Fetch users ready for deletion
        List<Long> usersReadyForDeletion = userRepository.findUsersReadyForFullDeletion(LocalDateTime.now());

        if (usersReadyForDeletion.isEmpty()) {
            log.info("GdprCleanupJob: no users ready for full deletion today");
            return;
        }

        int usersDeleted = 0;
        int totalAudioDeleted = 0;

        // Step B: Sequenced FK-Safe Deletion Pipeline
        for (Long userId : usersReadyForDeletion) {
            try {
                // 1. Delete Audio Attempts first
                totalAudioDeleted += userRepository.deleteExpiredPronunciationAttempts(userId, LocalDateTime.now());
                
                // 2. Cascade down dependent tables
                userRepository.deleteTranslationRequestsByUserId(userId);
                userRepository.deleteUserProgressByUserId(userId);
                userRepository.deleteUserAchievementsByUserId(userId);
                userRepository.deleteGamificationProfileByUserId(userId);
                userRepository.deleteLearnerLanguagesByUserId(userId);
                userRepository.deleteLearnerProfilesByUserId(userId);
                userRepository.deleteDiagnosticAssessmentsByUserId(userId);
                
                // 3. Tombstone the Root User (to preserve user_consent legal hold)
                userRepository.anonymizeUser(userId);
                
                usersDeleted++;
                log.info("GdprCleanupJob: full deletion complete and tombstoned for userId={}", userId);
            } catch (Exception e) {
                log.error("GdprCleanupJob: failed to delete userId={}: {}", userId, e.getMessage());
            }
        }

        // Step C: Audit log
        if (usersDeleted > 0 || totalAudioDeleted > 0) {
            auditLogRepository.logGdprPurge(totalAudioDeleted, usersDeleted, LocalDateTime.now());
        }

        log.info("GdprCleanupJob: purge complete — {} audio records, {} users deleted", totalAudioDeleted, usersDeleted);
    }
}
