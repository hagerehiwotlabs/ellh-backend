-- V4: user_consent — GDPR consent audit trail
-- Section 4.5.2.2 — user_consent table specification
-- Section 4.5.2.7 — retained for 7 years even after account deletion

CREATE TABLE user_consent (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    consent_type    VARCHAR(50)     NOT NULL
                        CHECK (consent_type IN (
                            'PRIVACY_POLICY', 'DATA_COLLECTION',
                            'AUDIO_RECORDING', 'RESEARCH_USE'
                        )),
    policy_version  VARCHAR(20)     NOT NULL,
    granted_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    ip_address      INET            NOT NULL,
    UNIQUE (user_id, consent_type, policy_version)
);

-- ON DELETE RESTRICT: user_consent rows must be retained even after
-- account deletion (7-year legal hold per Section 4.5.2.7)
COMMENT ON TABLE user_consent IS '7-year legal hold: ON DELETE RESTRICT prevents CASCADE delete';
