-- V7: exercises — atomic learning tasks within lessons
-- Section 4.5.2.2 — exercises table specification

CREATE TABLE exercises (
    id              BIGSERIAL       PRIMARY KEY,
    lesson_id       BIGINT          NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    exercise_type   VARCHAR(30)     NOT NULL
                        CHECK (exercise_type IN (
                            'MULTIPLE_CHOICE', 'PRONUNCIATION', 'TRANSLATION',
                            'FILL_BLANK', 'LISTENING', 'WRITING'
                        )),
    question_text   TEXT            NOT NULL,
    correct_answer  TEXT            NOT NULL,
    options         JSONB,          -- [{id, text, isCorrect}] for MULTIPLE_CHOICE
    hint_text       TEXT,
    audio_url       TEXT,
    image_url       TEXT,
    order_index     INTEGER         NOT NULL,
    points          INTEGER         NOT NULL DEFAULT 10,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN exercises.options IS 'JSONB: [{id: string, text: string, isCorrect: boolean}] — NULL for non-MULTIPLE_CHOICE types';
COMMENT ON COLUMN exercises.audio_url IS 'Cloud storage URL — used by LISTENING exercise type';
