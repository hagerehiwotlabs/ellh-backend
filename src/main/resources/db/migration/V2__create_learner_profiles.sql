-- V2__create_languages.sql
CREATE TABLE languages (
    id               BIGSERIAL       PRIMARY KEY,
    name             VARCHAR(100)    NOT NULL UNIQUE,
    iso_code         VARCHAR(3)      NOT NULL UNIQUE
        CHECK (iso_code ~ '^[a-z]{3}$'),
    script_type      VARCHAR(20)     NOT NULL
        CHECK (script_type IN ('GEEZ_FIDEL','LATIN_QUBEE')),
    native_name      VARCHAR(100)    NOT NULL,
    is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
    total_speakers   BIGINT,
    flag_icon        VARCHAR(255),
    description      TEXT
);

-- Seed the 3 supported languages immediately.
-- Adding a language later = INSERT only, no code change (Design Goal b: Modifiability).
INSERT INTO languages (name, iso_code, script_type, native_name, total_speakers)
VALUES
    ('Amharic',     'amh', 'GEEZ_FIDEL',  'አማርኛ',      57000000),
    ('Tigrigna',    'tir', 'GEEZ_FIDEL',  'ትግርኛ',      9700000),
    ('Afaan Oromo', 'orm', 'LATIN_QUBEE', 'Afaan Oromoo', 37000000);

