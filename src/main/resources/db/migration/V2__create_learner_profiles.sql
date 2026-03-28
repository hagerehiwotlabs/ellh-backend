-- V2: learner_profiles — per-learner configuration (pathway, CEFR, daily goal)
-- Section 4.5.2.2 — learner_profiles table specification

CREATE TABLE learner_profiles (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    native_language_id      BIGINT,         -- FK → languages.id added in V5 via ALTER
    target_language_id      BIGINT,         -- FK → languages.id added in V5 via ALTER
    pathway_type            VARCHAR(20)     NOT NULL
                                CHECK (pathway_type IN ('FOREIGN_LEARNER', 'BILINGUAL_LEARNER')),
    current_cefr_level      VARCHAR(5)      NOT NULL DEFAULT 'A1'
                                CHECK (current_cefr_level IN ('A1', 'A2', 'B1')),
    onboarding_complete     BOOLEAN         NOT NULL DEFAULT FALSE,
    daily_goal_minutes      INTEGER         NOT NULL DEFAULT 10,
    notifications_enabled   BOOLEAN         NOT NULL DEFAULT TRUE,
    diagnostic_score        INTEGER,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN learner_profiles.native_language_id IS 'NULL for foreign learners with no Ethiopian native language';
COMMENT ON COLUMN learner_profiles.pathway_type       IS 'Set by DiagnosticAssessment.evaluate() in Sprint 3';
