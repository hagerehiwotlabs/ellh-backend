-- V12__create_achievements.sql
CREATE TABLE achievements (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT         NOT NULL,
    icon_url        TEXT,
    xp_reward       INTEGER      NOT NULL DEFAULT 50 CHECK (xp_reward >= 0),
    criteria_type   VARCHAR(40)  NOT NULL
        CHECK (criteria_type IN ('LESSON_COMPLETE','STREAK_DAYS',
                                 'XP_TOTAL','EXERCISE_SCORE',
                                 'LANGUAGE_MASTERY')),
    criteria_value  JSONB        NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE user_achievements (
    id              BIGSERIAL   PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  BIGINT      NOT NULL REFERENCES achievements(id),
    awarded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    progress_data   JSONB,
    is_completed    BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (user_id, achievement_id)
);

-- Seed 5 starter achievements
INSERT INTO achievements (name, description, xp_reward, criteria_type, criteria_value)
VALUES
  ('First Step',
   'Complete your first lesson',
   100, 'LESSON_COMPLETE', '{"count": 1}'),
  ('Week Warrior',
   'Maintain a 7-day learning streak',
   200, 'STREAK_DAYS', '{"count": 7}'),
  ('Century Club',
   'Earn 100 XP total',
   50,  'XP_TOTAL', '{"threshold": 100}'),
  ('Perfect Score',
   'Score 100% on any exercise',
   75,  'EXERCISE_SCORE', '{"score": 100}'),
  ('Language Explorer',
   'Start learning a second language',
   150, 'LESSON_COMPLETE', '{"count": 1, "unique_languages": 2}');

