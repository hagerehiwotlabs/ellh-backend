package com.ellh.sync.entity;

/**
 * Types of offline actions queued in the Android SQLite sync_queue.
 * Section 4.5.2.5 — sync_queue.action_type column.
 * PROGRESS_UPDATE and EXERCISE_COMPLETE have priority=1 (never purged).
 */
public enum ActionType {
    PROGRESS_UPDATE,
    EXERCISE_COMPLETE,
    LESSON_COMPLETE,
    TRANSLATION_REQUEST,
    FEEDBACK_SUBMIT
}
