package com.ellh.content.entity;

/**
 * Exercise types supported by the ELLH exercise engine.
 * Section 4.5.2.2 — exercises.exercise_type column CHECK constraint.
 * PRONUNCIATION and TRANSLATION require AI (SS-5) and are unavailable offline.
 */
public enum ExerciseType {
    MULTIPLE_CHOICE,
    PRONUNCIATION,
    TRANSLATION,
    FILL_BLANK,
    LISTENING,
    WRITING
}
