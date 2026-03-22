-- V4__create_diagnostic_assessments.sql
CREATE TABLE diagnostic_assessments (
    id                       BIGSERIAL   PRIMARY KEY,
    user_id                  BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    responses                JSONB       NOT NULL,
    assigned_pathway         VARCHAR(20) NOT NULL
        CHECK (assigned_pathway IN ('FOREIGN_LEARNER','BILINGUAL_LEARNER')),
    assigned_cefr_level      VARCHAR(5)  NOT NULL
        CHECK (assigned_cefr_level IN ('A1','A2','B1')),
    total_score              INTEGER     NOT NULL
        CHECK (total_score >= 0 AND total_score <= 100),
    language_knowledge_flags JSONB,
    completed_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN diagnostic_assessments.responses IS
  'Array of {questionId, answer, correct} — serialised from DiagnosticAssessmentFragment.';

