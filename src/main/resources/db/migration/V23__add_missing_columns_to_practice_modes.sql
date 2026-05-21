-- Add all columns expected by PracticeMode entity that are not yet in the table

ALTER TABLE practice_modes
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'VOCABULARY',
    ADD COLUMN IF NOT EXISTS question_count INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS estimated_duration_minutes INTEGER NOT NULL DEFAULT 5,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS language_id BIGINT;

-- Add foreign key to languages table if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'languages') THEN
        ALTER TABLE practice_modes
            ADD CONSTRAINT fk_practice_modes_language
            FOREIGN KEY (language_id) REFERENCES languages (id) ON DELETE CASCADE;
    END IF;
END $$;