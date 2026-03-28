package com.ellh.learning.entity;

/**
 * Completion state of a lesson or exercise for a given learner.
 * Section 4.5.2.2 — user_progress.status column.
 */
public enum ProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    MASTERED
}
