package com.ellh.feedback.entity;

import com.ellh.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User-submitted and system-generated issue reports.
 * Section 4.5.1 — Feedback & Audit Domain (FeedbackReport entity).
 * Section 4.5.2.2 — feedback_reports table specification.
 *
 * Two sources of reports:
 *   1. FeedbackService — user-submitted via SCR-13 In-App Feedback Form
 *   2. FeedbackReporter — system-generated for AI failures and sync errors
 *      (user_id is NULL for system-generated reports)
 *
 * Severity HIGH is automatically assigned by FeedbackReporter for AI and sync
 * failures. ContentAdmins can adjust severity via the admin dashboard.
 *
 * Reports are NEVER auto-deleted (no TTL). Status lifecycle:
 *   OPEN → IN_REVIEW → RESOLVED | DISMISSED
 */
@Entity
@Table(name = "feedback_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NULLABLE — null for FeedbackReporter system-generated reports.
     * ON DELETE SET NULL: when a user account is deleted, the report is
     * anonymised (user_id set to null) — the report itself is kept.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    /** ID of the affected content item. Null for system-level reports. */
    @Column(name = "target_id", length = 50)
    private String targetId;

    /**
     * Issue category — free-form string rather than enum to allow new types
     * without a migration. Values: INCORRECT_ANSWER | AUDIO_PROBLEM |
     * TRANSLATION_ERROR | AI_INACCURACY | SYNC_FAILURE | APP_CRASH | OTHER
     */
    @Column(name = "issue_type", nullable = false, length = 40)
    private String issueType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    @Builder.Default
    private FeedbackSeverity severity = FeedbackSeverity.LOW;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
}
