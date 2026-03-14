-- Repeatable migration (R__ prefix) — re-runs if checksum changes
-- Seeds the three Ethiopian languages required by Section 4.5.2.2
-- ISO 639-3 codes: amh (Amharic), tir (Tigrigna), orm (Afaan Oromo)

INSERT INTO languages (name, iso_code, script_type, native_name, is_active)
VALUES
  ('Amharic',      'amh', 'GEEZ_FIDEL',  'አማርኛ',    true),
  ('Tigrigna',     'tir', 'GEEZ_FIDEL',  'ትግርኛ',    true),
  ('Afaan Oromo',  'orm', 'LATIN_QUBEE', 'Afaan Oromo', true)
ON CONFLICT (iso_code) DO NOTHING;
