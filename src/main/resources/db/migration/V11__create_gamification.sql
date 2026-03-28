-- V11: gamification_profiles + achievements + user_achievements
-- Section 4.5.2.2 — gamification tables specification

CREATE TABLE gamification_profiles (
    id                      BIGSERIAL   PRIMARY KEY,
    user_id                 BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    total_xp                INTEGER     NOT NULL DEFAULT 0,
    current_streak          INTEGER     NOT NULL DEFAULT 0,
    longest_streak          INTEGER     NOT NULL DEFAULT 0,
    last_activity_date      DATE,
    level                   INTEGER     NOT NULL DEFAULT 1,
    streak_freeze_available BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN gamification_profiles.level IS '3NF exception: denormalised for query performance; derived from total_xp via XP threshold table';

CREATE TABLE achievements (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    description     TEXT            NOT NULL,
    icon_url        TEXT,
    xp_reward       INTEGER         NOT NULL DEFAULT 50,
    criteria_type   VARCHAR(40)     NOT NULL
                        CHECK (criteria_type IN (
                            'LESSON_COMPLETE', 'STREAK_DAYS', 'XP_TOTAL',
                            'EXERCISE_SCORE', 'LANGUAGE_MASTERY'
                        )),
    criteria_value  JSONB           NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN achievements.criteria_value IS 'JSONB: {"type":"LESSON_COMPLETE","count":1} — evaluated by GamificationService (Sprint 8)';

CREATE TABLE user_achievements (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  BIGINT          NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    awarded_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    progress_data   JSONB,
    is_completed    BOOLEAN         NOT NULL DEFAULT TRUE,
    UNIQUE (user_id, achievement_id)
);
