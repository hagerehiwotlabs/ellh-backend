-- Clean up existing dummy exercises and lessons to prevent ID collisions
DELETE FROM exercises;
DELETE FROM lessons;

DO $$
DECLARE
    lang_amh BIGINT; lang_tir BIGINT; lang_orm BIGINT;
    lesson_id BIGINT;
BEGIN
    -- Retrieve actual seeded language IDs
    SELECT id INTO lang_amh FROM languages WHERE iso_code = 'amh';
    SELECT id INTO lang_tir FROM languages WHERE iso_code = 'tir';
    SELECT id INTO lang_orm FROM languages WHERE iso_code = 'orm';

    -- =========================================================================
    -- AMHARIC (አማርኛ) CURRICULUM
    -- =========================================================================

    -- Lesson 1: Greetings & Respect (ሰላምታ)
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES (lang_amh, 'Greetings & Respect (ሰላምታ)', 'Learn standard and respectful Amharic greetings.', 'A1', 1, 25, 8)
    RETURNING id INTO lesson_id;

    -- Exercises for Amharic Lesson 1
    INSERT INTO exercises (lesson_id, exercise_type, question_text, correct_answer, options, order_index, points)
    VALUES 
    (lesson_id, 'MULTIPLE_CHOICE', 'How do you say "Hello" in Amharic?', 'a', 
     '[{"id":"a","text":"ሰላም (Selam)","isCorrect":true},{"id":"b","text":"ቻው (Chaw)","isCorrect":false},{"id":"c","text":"እባክህ (Ebakih)","isCorrect":false}]'::jsonb, 1, 10),
    (lesson_id, 'MULTIPLE_CHOICE', 'What greeting is used specifically for a female?', 'b', 
     '[{"id":"a","text":"እንዴት ነህ? (Indet neh?)","isCorrect":false},{"id":"b","text":"እንዴት ነሽ? (Indet nesh?)","isCorrect":true},{"id":"c","text":"እንዴት ናችሁ? (Indet nachu?)","isCorrect":false}]'::jsonb, 2, 10),
    (lesson_id, 'FILL_BLANK', 'Complete the greeting of respect: "ጤና ይስጥልኝ" (_____ yistilign - May health be given to you for me.)', 'ጤና', NULL, 3, 10),
    (lesson_id, 'WRITING', 'Write "Thank you" in Amharic.', 'አመሰግናለሁ', NULL, 4, 10),
    (lesson_id, 'MULTIPLE_CHOICE', 'What is the polite reply to "እንዴት ነህ?" (How are you?)', 'a', 
     '[{"id":"a","text":"ደህና ነኝ (Dehna negn)","isCorrect":true},{"id":"b","text":"አዎ (Awo)","isCorrect":false},{"id":"c","text":"አይደለም (Aydellem)","isCorrect":false}]'::jsonb, 5, 10);


    -- Lesson 2: Introducing Yourself (ራስን ማስተዋወቅ)
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES (lang_amh, 'Introducing Yourself (ራስን ማስተዋወቅ)', 'Learn to say your name, heritage, and ask others their names.', 'A1', 2, 25, 10)
    RETURNING id INTO lesson_id;

    -- Exercises for Amharic Lesson 2
    INSERT INTO exercises (lesson_id, exercise_type, question_text, correct_answer, options, order_index, points)
    VALUES 
    (lesson_id, 'MULTIPLE_CHOICE', 'How do you say "My name is..."?', 'a', 
     '[{"id":"a","text":"ስሜ... ይባላል (Sime... yibalal)","isCorrect":true},{"id":"b","text":"ስምህ ማን ነው? (Simih man new?)","isCorrect":false}]'::jsonb, 1, 10),
    (lesson_id, 'FILL_BLANK', 'Complete: "ስም__ ማን ነው?" (What is your name? - to a male)', 'ህ', NULL, 2, 10);


    -- =========================================================================
    -- TIGRIGNA (ትግርኛ) CURRICULUM
    -- =========================================================================

    -- Lesson 1: Core Greetings (ሰላምታ)
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES (lang_tir, 'Core Greetings (ሰላምታ)', 'Learn everyday Tigrigna greetings.', 'A1', 1, 25, 8)
    RETURNING id INTO lesson_id;

    -- Exercises for Tigrigna Lesson 1
    INSERT INTO exercises (lesson_id, exercise_type, question_text, correct_answer, options, order_index, points)
    VALUES 
    (lesson_id, 'MULTIPLE_CHOICE', 'How do you say "How are you?" to a female in Tigrigna?', 'c', 
     '[{"id":"a","text":"ከመይ አሎኻ? (Kemey aloha?)","isCorrect":false},{"id":"b","text":"ከመይ አሎኹም? (Kemey alohum?)","isCorrect":false},{"id":"c","text":"ከመይ አሎኺ? (Kemey alohi?)","isCorrect":true}]'::jsonb, 1, 10),
    (lesson_id, 'FILL_BLANK', 'Complete the standard response: "ደሓን _____ " (Dehan ____ - I am fine.)', 'እየ', NULL, 2, 10);


    -- =========================================================================
    -- AFAAN OROMO CURRICULUM
    -- =========================================================================

    -- Lesson 1: Basic Greetings (Nagaa)
    INSERT INTO lessons (language_id, title, description, cefr_level, order_index, xp_reward, estimated_minutes)
    VALUES (lang_orm, 'Basic Greetings (Nagaa)', 'Learn essential greetings in Afaan Oromo.', 'A1', 1, 25, 8)
    RETURNING id INTO lesson_id;

    -- Exercises for Afaan Oromo Lesson 1
    INSERT INTO exercises (lesson_id, exercise_type, question_text, correct_answer, options, order_index, points)
    VALUES 
    (lesson_id, 'MULTIPLE_CHOICE', 'What is the word for "Peace" or used as a standard greeting?', 'a', 
     '[{"id":"a","text":"Nagaa","isCorrect":true},{"id":"b","text":"Akkam","isCorrect":false},{"id":"c","text":"Fayyaa","isCorrect":false}]'::jsonb, 1, 10),
    (lesson_id, 'FILL_BLANK', 'Complete: "Akkam _____?" (How are you?)', 'jirtu', NULL, 2, 10);

END $$;
