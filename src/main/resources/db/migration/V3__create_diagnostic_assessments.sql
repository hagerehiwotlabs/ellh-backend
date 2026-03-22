-- V3__create_learner_profiles.sql
CREATE TABLE learner_profiles (
    id                   BIGSERIAL   PRIMARY KEY,
    user_id              BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    native_language_id   BIGINT      REFERENCES languages(id),
    target_language_id   BIGINT      NOT NULL REFERENCES languages(id),
    pathway_type         VARCHAR(20) NOT NULL
        CHECK (pathway_type IN ('FOREIGN_LEARNER','BILINGUAL_LEARNER')),
    current_cefr_level   VARCHAR(5)  NOT NULL DEFAULT 'A1'
        CHECK (current_cefr_level IN ('A1','A2','B1')),
    onboarding_complete  BOOLEAN     NOT NULL DEFAULT FALSE,
    daily_goal_minutes   INTEGER     NOT NULL DEFAULT 10
        CHECK (daily_goal_minutes > 0),
    notifications_enabled BOOLEAN   NOT NULL DEFAULT TRUE,
    diagnostic_score     INTEGER
        CHECK (diagnostic_score IS NULL OR (diagnostic_score >= 0
               AND diagnostic_score <= 100)),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN learner_profiles.native_language_id IS
  'NULL for foreign learners with no Ethiopian language background.';

