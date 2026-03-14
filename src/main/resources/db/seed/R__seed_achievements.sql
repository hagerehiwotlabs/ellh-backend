-- Seeds initial achievement definitions
-- criteria_value uses JSONB: {"type":"LESSON_COMPLETE","count":1}

INSERT INTO achievements (name, description, xp_reward, criteria_type, criteria_value, is_active)
VALUES
  ('First Step',       'Complete your first lesson',           50,  'LESSON_COMPLETE',  '{"type":"LESSON_COMPLETE","count":1}',   true),
  ('On a Roll',        'Complete 5 lessons',                   100, 'LESSON_COMPLETE',  '{"type":"LESSON_COMPLETE","count":5}',   true),
  ('Week Warrior',     'Maintain a 7-day streak',              150, 'STREAK_DAYS',      '{"type":"STREAK_DAYS","count":7}',       true),
  ('Century Learner',  'Earn 100 XP',                          75,  'XP_TOTAL',         '{"type":"XP_TOTAL","count":100}',        true),
  ('Perfect Score',    'Score 100% on any exercise',           100, 'EXERCISE_SCORE',   '{"type":"EXERCISE_SCORE","score":100}',  true)
ON CONFLICT (name) DO NOTHING;
