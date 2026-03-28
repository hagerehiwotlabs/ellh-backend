-- V12: learner_languages — per-user per-language progress aggregate
-- Section 4.5.2.2 — learner_languages table specification

CREATE TABLE learner_languages (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_id         BIGINT          NOT NULL REFERENCES languages(id) ON DELETE RESTRICT,
    cefr_level          VARCHAR(5)      NOT NULL DEFAULT 'A1'
                            CHECK (cefr_level IN ('A1', 'A2', 'B1')),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    learning_start_date TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    total_xp_earned     INTEGER         NOT NULL DEFAULT 0,
    lessons_completed   INTEGER         NOT NULL DEFAULT 0,
    mastery_percent     DECIMAL(5,2)    NOT NULL DEFAULT 0.00,
    last_switch_date    TIMESTAMPTZ,
    UNIQUE (user_id, language_id)
);

COMMENT ON COLUMN learner_languages.mastery_percent IS '0.00–100.00 — recalculated by LearnerLanguageService after each lesson completion';
COMMENT ON COLUMN learner_languages.last_switch_date IS 'Set for BILINGUAL_LEARNER when switching active language; NULL for single-language learners';
