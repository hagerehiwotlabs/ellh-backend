package com.ellh.infrastructure.schedule;

import com.ellh.content.repository.ContentUpdateLogRepository;
import com.ellh.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GdprCleanupJob.
 *
 * Tests:
 *   1. Expired pronunciation_attempts are deleted on every run.
 *   2. When users are ready for full deletion, all data tables are cleaned.
 *   3. user_consent rows are never deleted during full user deletion.
 *   4. Audit log is written when records are deleted.
 *   5. When no users ready, no deletion methods called.
 *   6. Failure on one user does not stop processing of others.
 *
 * Section 4.5.2.7 GDPR lifecycle. NFR-13 privacy.
 */
@ExtendWith(MockitoExtension.class)
class GdprCleanupJobTest {

    @Mock private UserRepository             userRepository;
    @Mock private ContentUpdateLogRepository auditLogRepository;

    @InjectMocks
    private GdprCleanupJob cleanupJob;

    @Test
    void runDailyGdprPurge_alwaysDeletesExpiredAudioRecords() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(5);
        when(userRepository.findUsersReadyForFullDeletion())
                .thenReturn(Collections.emptyList());

        cleanupJob.runDailyGdprPurge();

        verify(userRepository).deleteExpiredPronunciationAttempts();
    }

    @Test
    void runDailyGdprPurge_withUsersReady_deletesAllUserDataTablesInOrder() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(0);
        when(userRepository.findUsersReadyForFullDeletion())
                .thenReturn(Arrays.asList(101L));

        cleanupJob.runDailyGdprPurge();

        // Verify FK-ordered deletion for userId=101
        var inOrder = inOrder(userRepository);
        inOrder.verify(userRepository).deleteTranslationRequestsByUserId(101L);
        inOrder.verify(userRepository).deleteUserProgressByUserId(101L);
        inOrder.verify(userRepository).deleteSyncQueueByUserId(101L);
        inOrder.verify(userRepository).deleteUserAchievementsByUserId(101L);
        inOrder.verify(userRepository).deleteGamificationProfileByUserId(101L);
        inOrder.verify(userRepository).deleteLearnerLanguagesByUserId(101L);
        inOrder.verify(userRepository).deleteLearnerProfileByUserId(101L);
        inOrder.verify(userRepository).deleteDiagnosticAssessmentByUserId(101L);
        inOrder.verify(userRepository).deleteUserById(101L);
    }

    @Test
    void runDailyGdprPurge_neverDeletesUserConsentRows() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(0);
        when(userRepository.findUsersReadyForFullDeletion())
                .thenReturn(Arrays.asList(101L));

        cleanupJob.runDailyGdprPurge();

        // CRITICAL: user_consent NEVER deleted
        verify(userRepository, never()).deleteUserConsentByUserId(any());
    }

    @Test
    void runDailyGdprPurge_writesAuditLogWhenRecordsDeleted() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(3);
        when(userRepository.findUsersReadyForFullDeletion()).thenReturn(Collections.emptyList());

        cleanupJob.runDailyGdprPurge();

        verify(auditLogRepository).logGdprPurge(eq(3), eq(0), any());
    }

    @Test
    void runDailyGdprPurge_noUsersReady_skipsFullDeletion() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(0);
        when(userRepository.findUsersReadyForFullDeletion()).thenReturn(Collections.emptyList());

        cleanupJob.runDailyGdprPurge();

        verify(userRepository, never()).deleteUserById(any());
    }

    @Test
    void runDailyGdprPurge_failureOnOneUser_continuesWithNext() {
        when(userRepository.deleteExpiredPronunciationAttempts()).thenReturn(0);
        when(userRepository.findUsersReadyForFullDeletion())
                .thenReturn(Arrays.asList(101L, 102L));
        doThrow(new RuntimeException("DB error"))
                .when(userRepository).deleteTranslationRequestsByUserId(101L);

        // Should not throw — failure on 101L is caught and 102L is processed
        cleanupJob.runDailyGdprPurge();

        verify(userRepository).deleteTranslationRequestsByUserId(102L);
    }
}
