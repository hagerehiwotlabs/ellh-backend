-- Repeatable migration (R__ prefix) — re-runs if checksum changes
-- Seeds the three Ethiopian languages required by Section 1.3.2
-- ISO 639-3 codes: amh (Amharic), tir (Tigrigna), orm (Afaan Oromo)
-- Design Goal a: ISO 639-3 codes used in ALL API responses
-- Design Goal b: adding a language = INSERT row only, no code change

INSERT INTO languages (name, iso_code, script_type, native_name, is_active, description)
VALUES
    ('Amharic',
     'amh',
     'GEEZ_FIDEL',
     'አማርኛ',
     TRUE,
     'Official working language of Ethiopia. Spoken by ~32 million as a first language.'),
    ('Tigrigna',
     'tir',
     'GEEZ_FIDEL',
     'ትግርኛ',
     TRUE,
     'Semitic language spoken in Tigray region of Ethiopia and Eritrea. ~10 million speakers.'),
    ('Afaan Oromo',
     'orm',
     'LATIN_QUBEE',
     'Afaan Oromo',
     TRUE,
     'Cushitic language, most widely spoken in Ethiopia. ~40 million speakers. Written in Qubee Latin script.')
ON CONFLICT (iso_code) DO UPDATE SET
    name        = EXCLUDED.name,
    native_name = EXCLUDED.native_name,
    is_active   = EXCLUDED.is_active,
    description = EXCLUDED.description;
