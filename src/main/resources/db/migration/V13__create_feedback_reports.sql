-- V13: feedback_reports — user-submitted and system-generated issue reports
-- Section 4.5.2.2 — feedback_reports table specification
-- user_id is NULLABLE to support FeedbackReporter system-generated entries

CREATE TABLE feedback_reports (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          REFERENCES users(id) ON DELETE SET NULL,
    target_type     VARCHAR(30)     NOT NULL
                        CHECK (target_type IN (
                            'LESSON', 'EXERCISE', 'TRANSLATION', 'PRONUNCIATION',
                            'AI_SERVICE', 'SYNC', 'APP_GENERAL'
                        )),
    target_id       VARCHAR(50),
    issue_type      VARCHAR(40)     NOT NULL,
    description     TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    severity        VARCHAR(10)     NOT NULL DEFAULT 'LOW'
                        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    resolution_notes TEXT
);

COMMENT ON COLUMN feedback_reports.user_id IS 'NULLABLE — NULL for FeedbackReporter system-generated AI/sync failure reports';
COMMENT ON COLUMN feedback_reports.severity IS 'HIGH auto-set by FeedbackReporter for AI and sync failures';
