-- V15: All indexes — Section 4.5.2.3 PostgreSQL Indexing Strategy
-- 15 indexes matching the exact specification from Chapter 4

-- users
CREATE UNIQUE INDEX idx_users_email
    ON users (email);
CREATE INDEX idx_users_user_type
    ON users (user_type);

-- learner_profiles
CREATE UNIQUE INDEX idx_learner_profiles_user_id
    ON learner_profiles (user_id);
CREATE INDEX idx_learner_profiles_target_language
    ON learner_profiles (target_language_id);

-- lessons
CREATE INDEX idx_lessons_lang_cefr
    ON lessons (language_id, cefr_level);
CREATE INDEX idx_lessons_lang_order
    ON lessons (language_id, order_index);

-- exercises
CREATE INDEX idx_exercises_lesson_id
    ON exercises (lesson_id);

-- user_progress
CREATE INDEX idx_user_progress_user_lesson
    ON user_progress (user_id, lesson_id);
CREATE INDEX idx_user_progress_user_status
    ON user_progress (user_id, status);

-- pronunciation_attempts
CREATE INDEX idx_pronunciation_user_id
    ON pronunciation_attempts (user_id);
CREATE INDEX idx_pronunciation_gdpr
    ON pronunciation_attempts (marked_for_deletion, retention_date);

-- gamification_profiles
CREATE UNIQUE INDEX idx_gamification_user_id
    ON gamification_profiles (user_id);

-- user_achievements
CREATE UNIQUE INDEX idx_user_achievements_user_achievement
    ON user_achievements (user_id, achievement_id);

-- feedback_reports
CREATE INDEX idx_feedback_status_severity
    ON feedback_reports (status, severity);

-- translation_requests
CREATE INDEX idx_translation_cache_lookup
    ON translation_requests (source_text, source_language, target_language);
