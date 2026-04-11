-- V16: Additional indexes for learner_languages not captured in V15
-- Sprint 2 discovery: learner_languages queries also need explicit indexes
-- for the BILINGUAL_LEARNER language-switch flow and mastery queries.
-- Section 4.5.2.3 — PostgreSQL Indexing Strategy.

-- Active language lookup per user (used in lesson dashboard header)
CREATE INDEX IF NOT EXISTS idx_learner_languages_user_active
    ON learner_languages (user_id, is_active);

-- Language-level mastery queries (progress dashboard, Section 4.5.3.4 SCR-10)
CREATE INDEX IF NOT EXISTS idx_learner_languages_language_mastery
    ON learner_languages (language_id, mastery_percent);

-- FYI: The primary UNIQUE index on (user_id, language_id) was already
-- created in V15 as idx_user_achievements_user_achievement (wrong name).
-- Correcting learner_languages unique constraint here:
CREATE UNIQUE INDEX IF NOT EXISTS idx_learner_languages_user_lang
    ON learner_languages (user_id, language_id);
