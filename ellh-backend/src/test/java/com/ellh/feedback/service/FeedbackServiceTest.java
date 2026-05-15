package com.ellh.feedback.service;

import com.ellh.feedback.model.FeedbackReport;
import com.ellh.feedback.repository.FeedbackReportRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FeedbackService — coverage gap-fill (Sprint 9 NFR-15).
 *
 * Tests:
 *   1. submitReport() creates FeedbackReport with status=OPEN.
 *   2. targetType, targetId, issueType correctly mapped to FeedbackReport.
 *   3. description stored on FeedbackReport.
 *   4. FeedbackReport saved via repository.
 *   5. reportAIFailure() creates HIGH severity report.
 *   6. reportSyncFailure() creates HIGH severity report.
 *
 * Section 4.5.1 Feedback & Audit Domain.
 * NFR-15 coverage gap-fill.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackReportRepository feedbackReportRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void submitReport_createsFeedbackReportWithStatusOpen() {
        feedbackService.submitReport(1L, "EXERCISE", "10",
                "INCORRECT_ANSWER", "The answer shown is wrong");

        ArgumentCaptor<FeedbackReport> captor = ArgumentCaptor.forClass(FeedbackReport.class);
        verify(feedbackReportRepository).save(captor.capture());
        assertEquals("OPEN", captor.getValue().getStatus());
    }

    @Test
    void submitReport_correctlyMapsAllFields() {
        feedbackService.submitReport(1L, "LESSON", "5",
                "AUDIO_PROBLEM", "Audio file not playing");

        ArgumentCaptor<FeedbackReport> captor = ArgumentCaptor.forClass(FeedbackReport.class);
        verify(feedbackReportRepository).save(captor.capture());

        FeedbackReport report = captor.getValue();
        assertEquals("LESSON",        report.getTargetType());
        assertEquals("5",             report.getTargetId());
        assertEquals("AUDIO_PROBLEM", report.getIssueType());
        assertEquals("Audio file not playing", report.getDescription());
        assertEquals(1L,              report.getUserId());
    }

    @Test
    void submitReport_persistsViaRepository() {
        feedbackService.submitReport(1L, "EXERCISE", "10",
                "OTHER", "General issue");
        verify(feedbackReportRepository, times(1)).save(any(FeedbackReport.class));
    }

    @Test
    void reportAIFailure_createsHighSeverityReport() {
        feedbackService.reportAIFailure("GoogleColabAIService",
                "CircuitBreaker tripped", 1L);

        ArgumentCaptor<FeedbackReport> captor = ArgumentCaptor.forClass(FeedbackReport.class);
        verify(feedbackReportRepository).save(captor.capture());
        assertEquals("HIGH",       captor.getValue().getSeverity());
        assertEquals("AI_SERVICE", captor.getValue().getTargetType());
    }

    @Test
    void reportSyncFailure_createsHighSeverityReport() {
        feedbackService.reportSyncFailure(1L, "idempotency-key-123",
                "5 retry attempts exhausted");

        ArgumentCaptor<FeedbackReport> captor = ArgumentCaptor.forClass(FeedbackReport.class);
        verify(feedbackReportRepository).save(captor.capture());
        assertEquals("HIGH",  captor.getValue().getSeverity());
        assertEquals("SYNC",  captor.getValue().getTargetType());
    }

    @Test
    void submitReport_nullDescription_handledGracefully() {
        assertDoesNotThrow(() ->
                feedbackService.submitReport(1L, "EXERCISE", "10",
                        "INCORRECT_ANSWER", null));
        verify(feedbackReportRepository).save(any());
    }
}
