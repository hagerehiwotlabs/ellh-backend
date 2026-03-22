-- V7__create_exercises.sql
CREATE TABLE exercises (
    id              BIGSERIAL    PRIMARY KEY,
    lesson_id       BIGINT       NOT NULL
        REFERENCES lessons(id) ON DELETE CASCADE,
    exercise_type   VARCHAR(30)  NOT NULL
        CHECK (exercise_type IN ('MULTIPLE_CHOICE','PRONUNCIATION',
                                 'TRANSLATION','FILL_BLANK',
                                 'LISTENING','WRITING')),
    question_text   TEXT         NOT NULL,
    correct_answer  TEXT         NOT NULL,
    options         JSONB,
    hint_text       TEXT,
    audio_url       TEXT,
    image_url       TEXT,
    order_index     INTEGER      NOT NULL CHECK (order_index >= 0),
    points          INTEGER      NOT NULL DEFAULT 10 CHECK (points >= 0),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN exercises.options IS
  'JSONB array [{id, text, isCorrect}]. Required for MULTIPLE_CHOICE. NULL for all others.';

