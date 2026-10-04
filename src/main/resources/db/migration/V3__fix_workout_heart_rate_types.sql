ALTER TABLE workouts
ALTER COLUMN average_heart_rate TYPE INTEGER
    USING average_heart_rate::INTEGER;

ALTER TABLE workouts
ALTER COLUMN max_heart_rate TYPE INTEGER
    USING max_heart_rate::INTEGER;