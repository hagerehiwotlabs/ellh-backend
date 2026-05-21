CREATE TABLE IF NOT EXISTS practice_answers (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question_text TEXT,
    user_answer TEXT,
    correct BOOLEAN,
    score INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);