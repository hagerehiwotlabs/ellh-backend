-- Seeds 9 lessons (3 per language) with 5 exercises each
-- Requires seeded languages (amh, tir, orm) from R__seed_languages.sql

DO $$
DECLARE
    lang_amh BIGINT; lang_tir BIGINT; lang_orm BIGINT;
BEGIN
    SELECT id INTO lang_amh FROM languages WHERE iso_code = 'amh';
    SELECT id INTO lang_tir FROM languages WHERE iso_code = 'tir';
    SELECT id INTO lang_orm FROM languages WHERE iso_code = 'orm';

    -- Amharic lessons
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES
        (lang_amh, 'Greetings & Introductions', 'Learn basic Amharic greetings', 'A1', 1, 25, 8),
        (lang_amh, 'Numbers & Counting', 'Master numbers 1-20', 'A1', 2, 25, 10),
        (lang_amh, 'Family & Relationships', 'Vocabulary for family members', 'A2', 3, 30, 12);

    -- Tigrigna lessons
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES
        (lang_tir, 'Tigrigna Greetings', 'Everyday greetings in Tigrigna', 'A1', 1, 25, 8),
        (lang_tir, 'Days & Months', 'Calendar vocabulary', 'A1', 2, 25, 10),
        (lang_tir, 'Food & Dining', 'Ordering food and common dishes', 'A2', 3, 30, 12);

    -- Afaan Oromo lessons
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES
        (lang_orm, 'Afaan Oromo Basics', 'Core greetings and phrases', 'A1', 1, 25, 8),
        (lang_orm, 'Colours & Shapes', 'Describing objects', 'A1', 2, 25, 10),
        (lang_orm, 'Travel & Directions', 'Asking for directions', 'A2', 3, 30, 12);
END $$;

-- For each lesson, insert 5 exercises (MULTIPLE_CHOICE and FILL_BLANK mix)
DO $$
DECLARE
    rec RECORD;
    lesson_order INT;
    ex_type TEXT;
BEGIN
    FOR rec IN SELECT id FROM lessons ORDER BY language_id, order_index LOOP
        lesson_order := 1;
        FOR ex_type IN SELECT unnest(ARRAY['MULTIPLE_CHOICE','FILL_BLANK','MULTIPLE_CHOICE','FILL_BLANK','MULTIPLE_CHOICE']) LOOP
            INSERT INTO exercises (lesson_id, exercise_type, question_text, correct_answer, options, order_index, points)
            VALUES (
                rec.id,
                ex_type,
                CASE WHEN ex_type = 'MULTIPLE_CHOICE' THEN 'What is the translation of "Hello"?' ELSE 'Fill in the blank: ___ means "thank you"."' END,
                CASE WHEN ex_type = 'MULTIPLE_CHOICE' THEN 'a' ELSE 'thank you' END,
                CASE WHEN ex_type = 'MULTIPLE_CHOICE' THEN
                    '[{"id":"a","text":"Hello","isCorrect":true},{"id":"b","text":"Goodbye","isCorrect":false},{"id":"c","text":"Please","isCorrect":false},{"id":"d","text":"Sorry","isCorrect":false}]'::jsonb
                ELSE NULL END,
                lesson_order,
                10
            );
            lesson_order := lesson_order + 1;
        END LOOP;
    END LOOP;
END $$;
