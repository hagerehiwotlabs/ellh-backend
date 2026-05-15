package com.ellh.infrastructure.schedule;

import com.ellh.user.repository.UserRepository;
import com.ellh.content.repository.ContentUpdateLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Daily scheduled job for GDPR data lifecycle management.
 *
 * Runs at 03:00 UTC every day (low-traffic window).
 * Implements GDPR deletion Step 5 from AccountDeletionService:
 * permanently deletes user data whose retention_date has passed.
 *
 * Deletion order respects FK constraints:
 *   1. pronunciation_attempts (marked_for_deletion=TRUE, retention_date < NOW())
 *   2. translation_requests
 *   3. user_progress + local_user_progress (via userId)
 *   4. sync_queue records
 *   5. user_achievements
 *   6. gamification_profiles
 *   7. learner_languages
 *   8. learner_profiles
 *   9. diagnostic_assessments
 *  10. users row (last — FK root; user_consent retained by design)
 *
 * Safety guard: only deletes rows where account_status=INACTIVE AND
 * retention_date IS NOT NULL AND retention_date < NOW().
 * This prevents accidental deletion of active accounts.
 *
 * Logging: records count of deleted records to content_update_logs
 * (audit trail required by Section 4.5.2.7).
 *
 * Section 4.5.2.7 — GDPR data lifecycle Step 5.
 * NFR-13 — Privacy: "audio recordings permanently deleted within 30 days
 * of an account deletion request."
 * Section 4.5.5.5 — Security enforcement point 3 (audio retention policy).
 */
@Component
public class GdprCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(GdprCleanupJob.class);

    private final UserRepository               userRepository;
    private final ContentUpdateLogRepository   auditLogRepository;

    public GdprCleanupJob(UserRepository userRepository,
                          ContentUpdateLogRepository auditLogRepository) {
        this.userRepository     = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Runs daily at 03:00 UTC.
     *
     * Deletes all pronunciation_attempts with:
     *   marked_for_deletion = TRUE AND retention_date < NOW()
     *
     * Then, for users with account_status=INACTIVE and no remaining
     * pronunciation_attempts (all purged), deletes all remaining user data.
     *
     * Safe to run on cold Supabase connections — uses HikariCP retry.
     * Idempotent: re-running on the same day has no effect (retention_date guard).
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void runDailyGdprPurge() {
        log.info("GdprCleanupJob: starting daily GDPR purge at {}", LocalDateTime.now());

        // ── Step A: Delete expired pronunciation_attempts ──────────────────
        int audioDeleted = userRepository.deleteExpiredPronunciationAttempts();
        log.info("GdprCleanupJob: deleted {} expired pronunciation_attempts", audioDeleted);

        // ── Step B: Find users ready for full deletion ─────────────────────
        // Users are ready when:
        //   - account_status = INACTIVE
        //   - retention_date < NOW()
        //   - no remaining pronunciation_attempts (all purged in Step A)
        java.util.List<Long> usersReadyForDeletion =
                userRepository.findUsersReadyForFullDeletion();

        if (usersReadyForDeletion.isEmpty()) {
            log.info("GdprCleanupJob: no users ready for full deletion today");
        }

        int usersDeleted = 0;
        for (Long userId : usersReadyForDeletion) {
            try {
                deleteAllUserData(userId);
                usersDeleted++;
                log.info("GdprCleanupJob: full deletion complete for userId={}", userId);
            } catch (Exception e) {
                log.error("GdprCleanupJob: failed to delete userId={}: {}",
                        userId, e.getMessage());
                // Continue with next user — partial failures are recoverable
            }
        }

        // ── Step C: Audit log ──────────────────────────────────────────────
        // Log deletion counts to content_update_logs for GDPR audit trail.
        if (audioDeleted > 0 || usersDeleted > 0) {
            auditLogRepository.logGdprPurge(audioDeleted, usersDeleted, LocalDateTime.now());
        }

        log.info("GdprCleanupJob: purge complete — {} audio records, {} users deleted",
                audioDeleted, usersDeleted);
    }

    /**
     * Deletes all remaining data for a user whose retention period has expired.
     * FK-ordered deletion ensures referential integrity throughout.
     *
     * user_consent rows are NEVER deleted (7-year legal hold).
     */
    @Transactional
    private void deleteAllUserData(Long userId) {
        userRepository.deleteTranslationRequestsByUserId(userId);
        userRepository.deleteUserProgressByUserId(userId);
        userRepository.deleteSyncQueueByUserId(userId);
        userRepository.deleteUserAchievementsByUserId(userId);
        userRepository.deleteGamificationProfileByUserId(userId);
        userRepository.deleteLearnerLanguagesByUserId(userId);
        userRepository.deleteLearnerProfileByUserId(userId);
        userRepository.deleteDiagnosticAssessmentByUserId(userId);
        // users row deleted last; user_consent rows are RETAINED
        userRepository.deleteUserById(userId);
    }
}
