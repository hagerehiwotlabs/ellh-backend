-- V10__create_translation_requests.sql
CREATE TABLE translation_requests (
    id                 BIGSERIAL   PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_text        TEXT        NOT NULL,
    target_text        TEXT        NOT NULL,
    source_language    VARCHAR(3)  NOT NULL
        CHECK (source_language ~ '^[a-z]{3}$'),
    target_language    VARCHAR(3)  NOT NULL
        CHECK (target_language ~ '^[a-z]{3}$'),
    cached_result      BOOLEAN     NOT NULL DEFAULT FALSE,
    processing_time_ms INTEGER     NOT NULL DEFAULT 0
        CHECK (processing_time_ms >= 0),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

