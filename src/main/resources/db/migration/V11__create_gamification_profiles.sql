-- V11__create_gamification_profiles.sql
CREATE TABLE gamification_profiles (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT    NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_xp               INTEGER   NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    current_streak         INTEGER   NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak         INTEGER   NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    last_activity_date     DATE,
    level                  INTEGER   NOT NULL DEFAULT 1 CHECK (level >= 1),
    streak_freeze_available BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN gamification_profiles.level IS
  'Denormalised — computed from total_xp by GamificationService. 3NF exception
   justified by query frequency (read on every dashboard load).';

