-- V5: languages — supported Ethiopian languages
-- Section 4.5.2.2 — languages table specification
-- Adding a new language requires only INSERT here (no code change) — Design Goal b.

CREATE TABLE languages (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    iso_code        VARCHAR(3)      NOT NULL UNIQUE,
    script_type     VARCHAR(20)     NOT NULL
                        CHECK (script_type IN ('GEEZ_FIDEL', 'LATIN_QUBEE')),
    native_name     VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    total_speakers  BIGINT,
    flag_icon       VARCHAR(255),
    description     TEXT
);

-- Add FK constraints to learner_profiles now that languages table exists
ALTER TABLE learner_profiles
    ADD CONSTRAINT fk_learner_native_lang
        FOREIGN KEY (native_language_id) REFERENCES languages(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_learner_target_lang
        FOREIGN KEY (target_language_id) REFERENCES languages(id) ON DELETE RESTRICT;

COMMENT ON TABLE languages IS 'Adding a new language = INSERT row only; no code change required (Design Goal b)';
COMMENT ON COLUMN languages.iso_code IS 'ISO 639-3: amh | tir | orm — used in all API responses (Design Goal a)';
