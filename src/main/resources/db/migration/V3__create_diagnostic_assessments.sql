-- V3: diagnostic_assessments — onboarding assessment results
-- Section 4.5.2.2 — diagnostic_assessments table specification

CREATE TABLE diagnostic_assessments (
    id                      BIGSERIAL       PRIMARY KEY,
    user_id                 BIGINT          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    responses               JSONB           NOT NULL,
    assigned_pathway        VARCHAR(20)     NOT NULL
                                CHECK (assigned_pathway IN ('FOREIGN_LEARNER', 'BILINGUAL_LEARNER')),
    assigned_cefr_level     VARCHAR(5)      NOT NULL
                                CHECK (assigned_cefr_level IN ('A1', 'A2', 'B1')),
    total_score             INTEGER         NOT NULL,
    language_knowledge_flags JSONB,
    completed_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN diagnostic_assessments.responses               IS 'JSONB: {questionId: answerValue} pairs';
COMMENT ON COLUMN diagnostic_assessments.language_knowledge_flags IS 'JSONB: {isoCode: hasFamiliarity} — used for BILINGUAL_LEARNER routing';
