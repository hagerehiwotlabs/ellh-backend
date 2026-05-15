-- =============================================================================
-- V18__add_fcm_token.sql
-- Sprint 8 backend requirement (documented in Sprint 9 — Flyway migration gap).
--
-- Adds fcm_token column to users table for Firebase Cloud Messaging.
-- Referenced by: Section 4.5.4.1 ND-09 Firebase Cloud Messaging.
--                NotificationService.sendAchievementUnlock(userId, achievement)
--                POST /api/v1/users/fcm-token (Android FirebaseTokenManager)
--
-- Section 4.5.5.5 security note: fcm_token is device-scoped, not personal data.
-- No GDPR retention policy required. Cleared on account deletion (Step 6).
-- =============================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512) NULL;

-- Index for fast lookup by token (NotificationService.sendToUser queries by userId
-- but bulk notification jobs may query by token directly in future versions)
CREATE INDEX IF NOT EXISTS idx_users_fcm_token
    ON users (fcm_token)
    WHERE fcm_token IS NOT NULL;

COMMENT ON COLUMN users.fcm_token IS
    'Firebase Cloud Messaging device token. Registered by Android app on login
     via POST /api/v1/users/fcm-token (FirebaseTokenManager). Cleared on
     account deletion. NULL if user has never registered a device or revoked
     notification permissions.';
