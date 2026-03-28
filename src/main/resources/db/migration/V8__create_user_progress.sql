-- V8: user_progress — per-user per-lesson per-exercise progress tracking
-- Section 4.5.2.2 — user_progress table specification

CREATE TABLE user_progress (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id           BIGINT          NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    exercise_id         BIGINT          REFERENCES exercises(id) ON DELETE SET NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED'
                            CHECK (status IN (
                                'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'MASTERED'
                            )),
    score               INTEGER,
    attempts            INTEGER         NOT NULL DEFAULT 0,
    time_spent_seconds  INTEGER,
    completed_at        TIMESTAMPTZ,
    last_attempt_at     TIMESTAMPTZ,
    UNIQUE (user_id, lesson_id, exercise_id)
);

COMMENT ON COLUMN user_progress.exercise_id IS 'NULL for lesson-level progress records; set for exercise-level records';
COMMENT ON COLUMN user_progress.score       IS '0–100 percentage score; NULL for non-scored exercise types';
