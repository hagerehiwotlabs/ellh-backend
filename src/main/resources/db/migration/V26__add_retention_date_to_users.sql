-- V26: Add retention_date to users for GDPR deletion compliance
ALTER TABLE users ADD COLUMN retention_date TIMESTAMPTZ;

COMMENT ON COLUMN users.retention_date IS 'GDPR: The timestamp after which all user personal data will be completely purged.';
