package com.ellh.sync.service;

import com.ellh.sync.model.SyncEvent;
import com.ellh.sync.model.SyncBatchRequest;
import com.ellh.sync.model.SyncBatchResponse;
import com.ellh.user.service.UserProgressService;
import com.ellh.gamification.service.GamificationService;
import com.ellh.feedback.service.FeedbackService;
import com.ellh.infrastructure.cache.IdempotencyCache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SyncService — coverage gap-fill (Sprint 9 NFR-15).
 *
 * Tests cover:
 *   1. PROGRESS_UPDATE event calls UserProgressService.updateProgress().
 *   2. LESSON_COMPLETE event calls GamificationService.awardXP().
 *   3. FEEDBACK_SUBMIT event calls FeedbackService.submitReport().
 *   4. Duplicate idempotency key → event skipped, skippedCount incremented.
 *   5. Partial batch: failure on one event does not stop others.
 *   6. processedCount + skippedCount sum equals total event count.
 *   7. ConflictResolver: server-wins when event createdAt < server record timestamp.
 *   8. Batch with empty event list returns zero counts without errors.
 *
 * Section 4.5.5.4 SYNC-01. Section 4.4 SS-6.
 * NFR-15 — ≥70% unit test coverage.
 */
@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock private UserProgressService userProgressService;
    @Mock private GamificationService gamificationService;
    @Mock private FeedbackService     feedbackService;
    @Mock private IdempotencyCache    idempotencyCache;

    @InjectMocks
    private SyncService syncService;

    @Test
    void processEvents_progressUpdate_callsUserProgressService() {
        SyncEvent event = buildEvent("PROGRESS_UPDATE",
                "{\"lessonId\":1,\"exerciseId\":10,\"score\":90,\"status\":\"COMPLETED\"}");
        when(idempotencyCache.isProcessed(event.getIdempotencyKey())).thenReturn(false);

        SyncBatchResponse response = syncService.processBatch(buildRequest(event));

        verify(userProgressService).updateProgress(anyLong(), anyLong(), anyLong(),
                anyString(), anyInt());
        assertEquals(1, response.getProcessedCount());
    }

    @Test
    void processEvents_lessonComplete_callsGamificationService() {
        SyncEvent event = buildEvent("LESSON_COMPLETE",
                "{\"lessonId\":1,\"xpEarned\":25}");
        when(idempotencyCache.isProcessed(event.getIdempotencyKey())).thenReturn(false);

        syncService.processBatch(buildRequest(event));

        verify(gamificationService).awardXP(anyLong(), eq(25), eq("LESSON_COMPLETE"));
    }

    @Test
    void processEvents_feedbackSubmit_callsFeedbackService() {
        SyncEvent event = buildEvent("FEEDBACK_SUBMIT",
                "{\"targetType\":\"EXERCISE\",\"targetId\":\"5\"," +
                "\"issueType\":\"INCORRECT_ANSWER\",\"description\":\"Wrong answer\"}");
        when(idempotencyCache.isProcessed(event.getIdempotencyKey())).thenReturn(false);

        syncService.processBatch(buildRequest(event));

        verify(feedbackService).submitReport(anyLong(), eq("EXERCISE"), eq("5"),
                eq("INCORRECT_ANSWER"), anyString());
    }

    @Test
    void processEvents_duplicateIdempotencyKey_skipsEvent() {
        SyncEvent event = buildEvent("PROGRESS_UPDATE", "{}");
        when(idempotencyCache.isProcessed(event.getIdempotencyKey())).thenReturn(true);

        SyncBatchResponse response = syncService.processBatch(buildRequest(event));

        verify(userProgressService, never()).updateProgress(any(), any(), any(), any(), any());
        assertEquals(0, response.getProcessedCount());
        assertEquals(1, response.getSkippedCount());
    }

    @Test
    void processEvents_processedPlusSkippedEqualsTotal() {
        SyncEvent e1 = buildEvent("PROGRESS_UPDATE", "{}");
        SyncEvent e2 = buildEvent("LESSON_COMPLETE", "{\"lessonId\":1,\"xpEarned\":10}");
        when(idempotencyCache.isProcessed(e1.getIdempotencyKey())).thenReturn(true);
        when(idempotencyCache.isProcessed(e2.getIdempotencyKey())).thenReturn(false);

        SyncBatchResponse response = syncService.processBatch(buildRequest(e1, e2));

        assertEquals(2, response.getProcessedCount() + response.getSkippedCount());
    }

    @Test
    void processEvents_emptyBatch_returnsZeroCounts() {
        SyncBatchRequest request = new SyncBatchRequest();
        request.setEvents(Collections.emptyList());
        request.setBatchId(UUID.randomUUID().toString());

        SyncBatchResponse response = syncService.processBatch(request);

        assertEquals(0, response.getProcessedCount());
        assertEquals(0, response.getSkippedCount());
    }

    @Test
    void processEvents_partialFailure_continuesProcessingNextEvent() {
        SyncEvent e1 = buildEvent("PROGRESS_UPDATE", "{}");
        SyncEvent e2 = buildEvent("LESSON_COMPLETE", "{\"lessonId\":1,\"xpEarned\":10}");

        when(idempotencyCache.isProcessed(anyString())).thenReturn(false);
        doThrow(new RuntimeException("DB error"))
                .when(userProgressService)
                .updateProgress(any(), any(), any(), any(), any());

        // Should not throw; second event should still be processed
        assertDoesNotThrow(() -> syncService.processBatch(buildRequest(e1, e2)));
        verify(gamificationService).awardXP(anyLong(), anyInt(), anyString());
    }

    @Test
    void processEvents_conflictResolution_serverWinsWhenEventIsStale() {
        // ConflictResolver: server record last_attempt_at > SyncEvent.createdAt → skip
        SyncEvent event = buildEvent("PROGRESS_UPDATE",
                "{\"lessonId\":1,\"exerciseId\":10,\"score\":80}");
        // createdAt set to 2 days ago — server record will be "newer"
        event.setCreatedAt(Instant.now().minusSeconds(172800).toEpochMilli());
        when(idempotencyCache.isProcessed(event.getIdempotencyKey())).thenReturn(false);
        when(userProgressService.getLastAttemptTimestamp(anyLong(), anyLong()))
                .thenReturn(Instant.now().toEpochMilli()); // server record = now (newer)

        SyncBatchResponse response = syncService.processBatch(buildRequest(event));

        assertEquals(1, response.getConflictCount(), "Stale event should be counted as conflict");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private SyncEvent buildEvent(String actionType, String payload) {
        SyncEvent e = new SyncEvent();
        e.setIdempotencyKey(UUID.randomUUID().toString());
        e.setActionType(actionType);
        e.setPayload(payload);
        e.setCreatedAt(Instant.now().toEpochMilli());
        e.setPriority(1);
        return e;
    }

    private SyncBatchRequest buildRequest(SyncEvent... events) {
        SyncBatchRequest req = new SyncBatchRequest();
        req.setEvents(Arrays.asList(events));
        req.setBatchId(UUID.randomUUID().toString());
        return req;
    }
}
