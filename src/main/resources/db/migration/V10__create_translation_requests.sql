-- V10: translation_requests — AI translation audit log and cache source
-- Section 4.5.2.2 — translation_requests table specification

CREATE TABLE translation_requests (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_text         TEXT            NOT NULL,
    target_text         TEXT            NOT NULL,
    source_language     VARCHAR(3)      NOT NULL,
    target_language     VARCHAR(3)      NOT NULL,
    cached_result       BOOLEAN         NOT NULL DEFAULT FALSE,
    processing_time_ms  INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN translation_requests.source_language IS 'ISO 639-3 code: amh | tir | orm | eng';
COMMENT ON COLUMN translation_requests.cached_result   IS 'TRUE if served from Redis cache (processing_time_ms=0 in that case)';
