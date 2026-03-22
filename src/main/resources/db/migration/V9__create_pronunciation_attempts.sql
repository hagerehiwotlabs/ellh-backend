-- V9__create_pronunciation_attempts.sql
CREATE TABLE pronunciation_attempts (
    id                   BIGSERIAL      PRIMARY KEY,
    user_id              BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id          BIGINT         NOT NULL REFERENCES exercises(id),
    audio_url            TEXT           NOT NULL,
    confidence_score     DECIMAL(5,4)   NOT NULL
        CHECK (confidence_score >= 0 AND confidence_score <= 1),
    feedback_text        TEXT           NOT NULL,
    processing_time_ms   INTEGER        NOT NULL CHECK (processing_time_ms >= 0),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    -- GDPR fields (Section 4.5.2.7)
    marked_for_deletion  BOOLEAN        NOT NULL DEFAULT FALSE,
    retention_date       TIMESTAMPTZ
);

COMMENT ON COLUMN pronunciation_attempts.marked_for_deletion IS
  'Set TRUE when user submits account deletion request (EC-08).';
COMMENT ON COLUMN pronunciation_attempts.retention_date IS
  'Set to NOW()+30days on deletion request. Cleanup job deletes when past this date.';

