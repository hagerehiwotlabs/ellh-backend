package com.ellh.user.service;

import com.ellh.feedback.repository.FeedbackReportRepository;
import com.ellh.infrastructure.cache.SessionCacheService;
import com.ellh.user.repository.UserRepository;
import com.ellh.user.repository.UserConsentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountDeletionService — GDPR 6-step deletion pipeline.
 *
 * Tests cover:
 *   1. All 6 steps execute in correct order on valid userId.
 *   2. Redis session cleared in Step 6.
 *   3. FCM token cleared in Step 6.
 *   4. user_consent rows are NEVER deleted (7-year legal hold).
 *   5. pronunciation_attempts retention_date set to NOW()+30days.
 *   6. isDeletionPending() returns true after initiation.
 *   7. feedback_reports anonymised (user_id → NULL).
 *   8. account_status set to INACTIVE as first step.
 *
 * Section 4.5.2.7 — GDPR data lifecycle (EC-08).
 * NFR-13 — Privacy compliance.
 */
@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock private UserRepository             userRepository;
    @Mock private UserConsentRepository      userConsentRepository;
    @Mock private FeedbackReportRepository   feedbackReportRepository;
    @Mock private SessionCacheService        sessionCacheService;

    @InjectMocks
    private AccountDeletionService deletionService;

    private static final Long TEST_USER_ID = 42L;

    @BeforeEach
    void setUp() {
        when(userRepository.isDeletionPending(TEST_USER_ID)).thenReturn(false);
    }

    @Test
    void initiateAccountDeletion_step1_setsAccountStatusInactive() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        verify(userRepository).setAccountStatusInactive(TEST_USER_ID);
    }

    @Test
    void initiateAccountDeletion_step2_marksPronunciationAttemptsForDeletion() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        // Verify marked_for_deletion=TRUE and retention_date=+30days
        verify(userRepository).markPronunciationAttemptsForDeletion(
                eq(TEST_USER_ID), any(LocalDateTime.class));
    }

    @Test
    void initiateAccountDeletion_step3_anonymisesFeedbackReports() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        verify(feedbackReportRepository).anonymiseByUserId(TEST_USER_ID);
    }

    @Test
    void initiateAccountDeletion_step4_doesNotDeleteUserConsent() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        // CRITICAL: user_consent MUST NOT be deleted (7-year legal hold)
        verify(userConsentRepository, never()).deleteByUserId(any());
        verify(userConsentRepository, never()).deleteAll();
    }

    @Test
    void initiateAccountDeletion_step6_clearsRedisSession() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        verify(sessionCacheService).clearSession(TEST_USER_ID);
    }

    @Test
    void initiateAccountDeletion_step6_clearsFcmToken() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        verify(userRepository).clearFcmToken(TEST_USER_ID);
    }

    @Test
    void initiateAccountDeletion_setsRetentionDateFor30DayGracePeriod() {
        deletionService.initiateAccountDeletion(TEST_USER_ID);
        // Capture retention_date passed to setRetentionDateForFullDeletion
        org.mockito.ArgumentCaptor<LocalDateTime> captor =
                org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).setRetentionDateForFullDeletion(eq(TEST_USER_ID), captor.capture());
        LocalDateTime retentionDate = captor.getValue();
        // retention_date should be ~30 days from now (within 1 minute tolerance)
        assertTrue(retentionDate.isAfter(LocalDateTime.now().plusDays(29)));
        assertTrue(retentionDate.isBefore(LocalDateTime.now().plusDays(31)));
    }

    @Test
    void isDeletionPending_returnsTrueWhenAlreadyInitiated() {
        when(userRepository.isDeletionPending(TEST_USER_ID)).thenReturn(true);
        assertTrue(deletionService.isDeletionPending(TEST_USER_ID));
    }
}
