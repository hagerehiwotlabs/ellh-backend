-- V9: pronunciation_attempts — AI pronunciation scoring records with GDPR fields
-- Section 4.5.2.2 — pronunciation_attempts table specification
-- Section 4.5.2.7 — GDPR: marked_for_deletion + retention_date

CREATE TABLE pronunciation_attempts (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id         BIGINT          NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    audio_url           TEXT            NOT NULL,
    confidence_score    DECIMAL(5,4)    NOT NULL CHECK (confidence_score BETWEEN 0 AND 1),
    feedback_text       TEXT            NOT NULL,
    processing_time_ms  INTEGER         NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    -- GDPR fields (Section 4.5.2.7)
    marked_for_deletion BOOLEAN         NOT NULL DEFAULT FALSE,
    retention_date      TIMESTAMPTZ
);

COMMENT ON COLUMN pronunciation_attempts.confidence_score    IS '0.0000–1.0000 AI-generated score';
COMMENT ON COLUMN pronunciation_attempts.marked_for_deletion IS 'Set TRUE on account deletion request (GDPR EC-08 Step 2)';
COMMENT ON COLUMN pronunciation_attempts.retention_date      IS 'NOW()+30days on deletion request; GDPR cleanup job deletes after this date';
