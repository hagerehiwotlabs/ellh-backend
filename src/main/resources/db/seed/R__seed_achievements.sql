-- Seeds initial achievement definitions for Sprint 1 testing
-- All 5 criteria_type values covered (tested by GamificationService in Sprint 8)
-- criteria_value JSONB must match the evaluator logic in GamificationService

INSERT INTO achievements (name, description, xp_reward, criteria_type, criteria_value, is_active)
VALUES
    ('First Step',
     'Complete your very first lesson',
     50,
     'LESSON_COMPLETE',
     '{"type":"LESSON_COMPLETE","count":1}',
     TRUE),
    ('On a Roll',
     'Complete 5 lessons',
     100,
     'LESSON_COMPLETE',
     '{"type":"LESSON_COMPLETE","count":5}',
     TRUE),
    ('Week Warrior',
     'Maintain a 7-day learning streak',
     150,
     'STREAK_DAYS',
     '{"type":"STREAK_DAYS","count":7}',
     TRUE),
    ('Century Learner',
     'Earn 100 XP',
     75,
     'XP_TOTAL',
     '{"type":"XP_TOTAL","count":100}',
     TRUE),
    ('Perfect Score',
     'Score 100% on any exercise',
     100,
     'EXERCISE_SCORE',
     '{"type":"EXERCISE_SCORE","score":100}',
     TRUE)
ON CONFLICT (name) DO NOTHING;
