-- V5__create_user_consent.sql
CREATE TABLE user_consent (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type    VARCHAR(50) NOT NULL
        CHECK (consent_type IN ('PRIVACY_POLICY','DATA_COLLECTION',
                                'AUDIO_RECORDING','RESEARCH_USE')),
    policy_version  VARCHAR(20) NOT NULL,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    ip_address      INET        NOT NULL,
    UNIQUE (user_id, consent_type, policy_version)
);

COMMENT ON TABLE user_consent IS
  'Retained for 7 years even after account deletion — legal requirement (Section 4.5.2.7).';

