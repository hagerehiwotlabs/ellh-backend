-- V14__create_learner_languages.sql
CREATE TABLE learner_languages (
    id                  BIGSERIAL      PRIMARY KEY,
    user_id             BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_id         BIGINT         NOT NULL REFERENCES languages(id),
    cefr_level          VARCHAR(5)     NOT NULL DEFAULT 'A1'
        CHECK (cefr_level IN ('A1','A2','B1')),
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    learning_start_date TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    total_xp_earned     INTEGER        NOT NULL DEFAULT 0 CHECK (total_xp_earned >= 0),
    lessons_completed   INTEGER        NOT NULL DEFAULT 0 CHECK (lessons_completed >= 0),
    mastery_percent     DECIMAL(5,2)   NOT NULL DEFAULT 0.00
        CHECK (mastery_percent >= 0 AND mastery_percent <= 100),
    last_switch_date    TIMESTAMPTZ,
    UNIQUE (user_id, language_id)
);

