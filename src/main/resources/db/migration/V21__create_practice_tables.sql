-- =============================================================================
-- Sprint 8 Practice module tables.
-- Creates practice_modes, practice_sessions, practice_answers in correct order.
-- =============================================================================

-- 1. Practice modes (must exist before sessions)
CREATE TABLE IF NOT EXISTS practice_modes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    difficulty VARCHAR(50),
    icon_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. Practice sessions (must exist before answers)
CREATE TABLE IF NOT EXISTS practice_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    mode_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    duration_seconds INTEGER,
    correct_answers INTEGER,
    total_questions INTEGER,
    score INTEGER,
    xp_earned INTEGER DEFAULT 0,
    passed BOOLEAN,
    feedback TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_practice_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_practice_sessions_mode
        FOREIGN KEY (mode_id) REFERENCES practice_modes (id) ON DELETE CASCADE
);

-- 3. Drop and recreate practice_answers with proper columns and foreign keys
DROP TABLE IF EXISTS practice_answers CASCADE;

CREATE TABLE practice_answers (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id VARCHAR(36) NOT NULL,
    user_answer TEXT,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_practice_answers_session
        FOREIGN KEY (session_id) REFERENCES practice_sessions (id) ON DELETE CASCADE
);

-- 4. Seed some practice modes for testing
INSERT INTO practice_modes (name, difficulty, icon_name) VALUES
    ('Flashcards', 'Beginner', 'ic_flashcards'),
    ('Quiz', 'Intermediate', 'ic_quiz'),
    ('Listening', 'Advanced', 'ic_headphones')
ON CONFLICT DO NOTHING;