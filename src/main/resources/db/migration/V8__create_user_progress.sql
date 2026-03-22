-- V8__create_user_progress.sql
CREATE TABLE user_progress (
    id                  BIGSERIAL   PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id           BIGINT      NOT NULL REFERENCES lessons(id),
    exercise_id         BIGINT      REFERENCES exercises(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','MASTERED')),
    score               INTEGER     CHECK (score IS NULL OR
                                          (score >= 0 AND score <= 100)),
    attempts            INTEGER     NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    time_spent_seconds  INTEGER     CHECK (time_spent_seconds IS NULL OR
                                          time_spent_seconds >= 0),
    completed_at        TIMESTAMPTZ,
    last_attempt_at     TIMESTAMPTZ,
    UNIQUE (user_id, lesson_id, exercise_id)
);

COMMENT ON COLUMN user_progress.exercise_id IS
  'NULL = lesson-level progress record. Non-null = exercise-level record.';

