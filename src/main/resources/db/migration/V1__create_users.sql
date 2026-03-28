-- V1: users table — STI root for all account types
-- Section 4.5.2.2 — users table specification
-- Section 4.5.2.1 — Single Table Inheritance with user_type discriminator

CREATE TABLE users (
    id                      BIGSERIAL       PRIMARY KEY,
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    password_hash           VARCHAR(255)    NOT NULL,
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    user_type               VARCHAR(30)     NOT NULL
                                CHECK (user_type IN (
                                    'FOREIGN_LEARNER',
                                    'BILINGUAL_LEARNER',
                                    'CONTENT_ADMIN',
                                    'SYSTEM_ADMIN'
                                )),
    account_status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_VERIFICATION'
                                CHECK (account_status IN (
                                    'ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'
                                )),
    auth_provider           VARCHAR(20)     NOT NULL DEFAULT 'LOCAL'
                                CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'APPLE', 'FACEBOOK')),
    email_verified          BOOLEAN         NOT NULL DEFAULT FALSE,
    profile_image_url       TEXT,
    failed_login_attempts   INTEGER         NOT NULL DEFAULT 0,
    fcm_token               VARCHAR(255),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_active             TIMESTAMPTZ
);

COMMENT ON TABLE  users                       IS 'STI root: FOREIGN_LEARNER | BILINGUAL_LEARNER | CONTENT_ADMIN | SYSTEM_ADMIN';
COMMENT ON COLUMN users.user_type             IS 'STI discriminator — maps to UserType enum';
COMMENT ON COLUMN users.failed_login_attempts IS 'Account locks when this reaches 5 (User.isAccountNonLocked)';
COMMENT ON COLUMN users.fcm_token             IS 'Firebase Cloud Messaging device token — populated in Sprint 8';
