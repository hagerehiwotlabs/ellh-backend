package com.ellh.feedback.entity;

/**
 * Section 4.5.2.2 — feedback_reports.severity column.
 * HIGH is automatically set for AI and sync failure reports created by FeedbackReporter.
 */
public enum FeedbackSeverity {
    LOW, MEDIUM, HIGH
}
