package com.ellh.user.service;

import com.ellh.user.repository.UserRepository;
import com.ellh.user.repository.UserConsentRepository;
import com.ellh.feedback.repository.FeedbackReportRepository;
import com.ellh.infrastructure.cache.SessionCacheService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implements the GDPR-compliant account deletion flow (EC-08).
 *
 * Six-step deletion pipeline (Section 4.5.2.7):
 *
 *   Step 1: Set users.account_status = INACTIVE.
 *           Prevents login immediately. Reversible within grace period.
 *
 *   Step 2: Mark all pronunciation_attempts for deletion.
 *           Sets marked_for_deletion=TRUE, retention_date=NOW()+30days.
 *           Actual deletion performed by GdprCleanupJob (daily @Scheduled).
 *           Satisfies the audio retention policy (Design Goal e, NFR-13).
 *
 *   Step 3: Anonymise feedback_reports.
 *           Sets user_id=NULL on all FeedbackReport rows for this user.
 *           Report content retained for content quality audit trail.
 *           Design Goal e: "user data protected in transit and at rest".
 *
 *   Step 4: RETAIN user_consent rows (7-year legal hold per GDPR Article 7).
 *           user_id FK is retained so consent history remains auditable.
 *           No deletion of consent records ever.
 *
 *   Step 5: Delete all remaining user data after 30-day grace period.
 *           Performed by GdprCleanupJob when retention_date < NOW().
 *           Includes: user_progress, gamification_profiles, user_achievements,
 *           learner_profiles, diagnostic_assessments, translation_requests,
 *           learner_languages, sync_queue records.
 *           users row itself deleted last to preserve FK integrity.
 *
 *   Step 6: Clear Redis session immediately.
 *           Prevents continued API access with existing JWT.
 *           Also clears fcm_token to stop FCM push delivery.
 *
 * Section 4.5.2.7 — GDPR data lifecycle.
 * Section 4.5.5.5 — Security enforcement point 3 (audio retention policy).
 * NFR-13 — Privacy compliance.
 * EC-08 — Delete Account functional requirement.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private static final int AUDIO_RETENTION_DAYS = 30;

    private final UserRepository             userRepository;
    private final UserConsentRepository      userConsentRepository;
    private final FeedbackReportRepository   feedbackReportRepository;
    private final SessionCacheService        sessionCacheService;

    /**
     * All dependencies injected via constructor (Design Goal g — Testability;
     * Trade-off i — Constructor DI; AIServiceGateway interface).
     */
    public AccountDeletionService(
            UserRepository userRepository,
            UserConsentRepository userConsentRepository,
            FeedbackReportRepository feedbackReportRepository,
            SessionCacheService sessionCacheService) {
        this.userRepository           = userRepository;
        this.userConsentRepository    = userConsentRepository;
        this.feedbackReportRepository = feedbackReportRepository;
        this.sessionCacheService      = sessionCacheService;
    }

    /**
     * Initiates the 6-step GDPR deletion pipeline for the given user.
     *
     * Steps 1, 2, 3, 6 execute synchronously within this method.
     * Steps 4 and 5 are handled by GdprCleanupJob (daily scheduled).
     *
     * @param userId  The user ID requesting account deletion.
     * @throws IllegalArgumentException if user not found.
     */
    @Transactional
    public void initiateAccountDeletion(Long userId) {
        log.info("GDPR deletion initiated for userId={}", userId);

        // ── Step 1: Set account_status = INACTIVE ──────────────────────────
        // Prevents login immediately. Existing JWT sessions are invalidated
        // in Step 6. Status can be reverted within 30 days if user changes mind.
        userRepository.setAccountStatusInactive(userId);
        log.info("GDPR Step 1 complete: userId={} account_status=INACTIVE", userId);

        // ── Step 2: Mark pronunciation_attempts for deletion ───────────────
        // Sets marked_for_deletion=TRUE and retention_date=NOW()+30days.
        // GdprCleanupJob deletes them daily once retention_date has passed.
        // Audio recordings are the most privacy-sensitive data (NFR-13).
        LocalDateTime retentionDate = LocalDateTime.now().plusDays(AUDIO_RETENTION_DAYS);
        userRepository.markPronunciationAttemptsForDeletion(userId, retentionDate);
        log.info("GDPR Step 2 complete: userId={} pronunciation_attempts marked, "
                + "retention_date={}", userId, retentionDate);

        // ── Step 3: Anonymise feedback_reports (user_id → NULL) ───────────
        // Content quality reports are retained for audit; user identity removed.
        // report_content, issue_type, and target_id retained for content team.
        feedbackReportRepository.anonymiseByUserId(userId);
        log.info("GDPR Step 3 complete: userId={} feedback_reports anonymised", userId);

        // ── Step 4: RETAIN user_consent rows (7-year legal hold) ──────────
        // Section 4.5.2.7: "retain user_consent rows (7-year legal hold)".
        // user_consent is never deleted — it is the auditable proof of consent.
        // No action taken here.
        log.info("GDPR Step 4: userId={} user_consent rows RETAINED (7-year legal hold)", userId);

        // ── Step 5: Mark user for full data deletion after grace period ────
        // GdprCleanupJob will delete remaining user data (progress, gamification,
        // learner_profile, diagnostic_assessments, translation_requests) after
        // retention_date has passed and pronunciation_attempts are deleted.
        userRepository.setRetentionDateForFullDeletion(userId, retentionDate);
        log.info("GDPR Step 5: userId={} full deletion scheduled for {}", userId, retentionDate);

        // ── Step 6: Clear Redis session + clear FCM token ─────────────────
        // Prevents continued API access with any existing JWT (Section 4.5.5.5).
        // Clears FCM token to stop push notification delivery immediately.
        sessionCacheService.clearSession(userId);
        userRepository.clearFcmToken(userId);
        log.info("GDPR Step 6 complete: userId={} Redis session cleared, FCM token cleared",
                userId);

        log.info("GDPR deletion pipeline initiated for userId={}. "
                + "Full data removal scheduled for {}", userId, retentionDate);
    }

    /**
     * Returns true if the given user has a pending deletion request
     * (account_status=INACTIVE and retention_date set).
     * Used by AccountController to prevent re-deletion requests.
     */
    public boolean isDeletionPending(Long userId) {
        return userRepository.isDeletionPending(userId);
    }
}
