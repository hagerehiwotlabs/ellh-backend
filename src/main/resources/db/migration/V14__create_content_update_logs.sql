-- V14: content_update_logs — immutable audit trail for content changes
-- Section 4.5.2.2 — content_update_logs table specification

CREATE TABLE content_update_logs (
    id              BIGSERIAL       PRIMARY KEY,
    content_type    VARCHAR(30)     NOT NULL
                        CHECK (content_type IN (
                            'LESSON', 'EXERCISE', 'LANGUAGE',
                            'ACHIEVEMENT', 'CONTRASTIVE_RULE'
                        )),
    content_id      VARCHAR(50)     NOT NULL,
    action          VARCHAR(20)     NOT NULL
                        CHECK (action IN (
                            'CREATE', 'UPDATE', 'DELETE', 'PUBLISH', 'DEPRECATE'
                        )),
    changed_by      BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    old_values      JSONB,
    new_values      JSONB           NOT NULL
);

COMMENT ON TABLE content_update_logs IS 'Immutable audit trail — rows are never deleted or updated';
COMMENT ON COLUMN content_update_logs.old_values IS 'NULL for CREATE actions; JSONB snapshot for UPDATE/DELETE';
