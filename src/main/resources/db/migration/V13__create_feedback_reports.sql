-- V13__create_feedback_and_audit.sql
CREATE TABLE feedback_reports (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       REFERENCES users(id),
    target_type       VARCHAR(30)  NOT NULL
        CHECK (target_type IN ('LESSON','EXERCISE','TRANSLATION',
                               'PRONUNCIATION','AI_SERVICE',
                               'SYNC','APP_GENERAL')),
    target_id         VARCHAR(50),
    issue_type        VARCHAR(40)  NOT NULL
        CHECK (issue_type IN ('INCORRECT_ANSWER','AUDIO_PROBLEM',
                              'TRANSLATION_ERROR','AI_INACCURACY',
                              'SYNC_FAILURE','APP_CRASH','OTHER')),
    description       TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','IN_REVIEW','RESOLVED','DISMISSED')),
    severity          VARCHAR(10)  NOT NULL DEFAULT 'LOW'
        CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at       TIMESTAMPTZ,
    resolution_notes  TEXT
);

COMMENT ON COLUMN feedback_reports.user_id IS
  'NULLABLE — allows FeedbackReporter service to create system-generated reports
   (AI failures, sync errors) with no associated user.';

CREATE TABLE content_update_logs (
    id            BIGSERIAL    PRIMARY KEY,
    content_type  VARCHAR(30)  NOT NULL
        CHECK (content_type IN ('LESSON','EXERCISE','LANGUAGE',
                                'ACHIEVEMENT','CONTRASTIVE_RULE')),
    content_id    VARCHAR(50)  NOT NULL,
    action        VARCHAR(20)  NOT NULL
        CHECK (action IN ('CREATE','UPDATE','DELETE','PUBLISH','DEPRECATE')),
    changed_by    BIGINT       NOT NULL REFERENCES users(id),
    changed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    old_values    JSONB,
    new_values    JSONB        NOT NULL
);

