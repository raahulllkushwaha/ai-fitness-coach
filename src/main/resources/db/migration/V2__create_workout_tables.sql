CREATE TABLE exercises (
                           id BIGSERIAL PRIMARY KEY,

                           name VARCHAR(150) NOT NULL,
                           normalized_name VARCHAR(150) NOT NULL,

                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,

                           CONSTRAINT uk_exercises_normalized_name
                               UNIQUE (normalized_name)
);


CREATE TABLE workouts (
                          id BIGSERIAL PRIMARY KEY,

                          user_id BIGINT NOT NULL,

                          workout_type VARCHAR(30) NOT NULL,

                          started_at TIMESTAMPTZ,
                          ended_at TIMESTAMPTZ,

                          duration_seconds BIGINT,

                          distance_meters DOUBLE PRECISION,
                          elevation_gain_meters DOUBLE PRECISION,

                          average_heart_rate SMALLINT,
                          max_heart_rate SMALLINT,

                          calories DOUBLE PRECISION,

                          average_pace_seconds_per_km DOUBLE PRECISION,

                          source VARCHAR(30) NOT NULL,

                          external_id VARCHAR(255),

                          notes TEXT,

                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,

                          CONSTRAINT fk_workouts_user
                              FOREIGN KEY (user_id)
                                  REFERENCES users(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT ck_workouts_duration
                              CHECK (duration_seconds IS NULL OR duration_seconds >= 0),

                          CONSTRAINT ck_workouts_distance
                              CHECK (distance_meters IS NULL OR distance_meters >= 0),

                          CONSTRAINT ck_workouts_heart_rate
                              CHECK (
                                  average_heart_rate IS NULL
                                      OR average_heart_rate > 0
                                  ),

                          CONSTRAINT ck_workouts_max_heart_rate
                              CHECK (
                                  max_heart_rate IS NULL
                                      OR max_heart_rate > 0
                                  )
);


CREATE TABLE workout_exercises (
                                   id BIGSERIAL PRIMARY KEY,

                                   workout_id BIGINT NOT NULL,
                                   exercise_id BIGINT NOT NULL,

                                   display_order INTEGER NOT NULL,

                                   created_at TIMESTAMPTZ NOT NULL,

                                   CONSTRAINT fk_workout_exercises_workout
                                       FOREIGN KEY (workout_id)
                                           REFERENCES workouts(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT fk_workout_exercises_exercise
                                       FOREIGN KEY (exercise_id)
                                           REFERENCES exercises(id),

                                   CONSTRAINT uk_workout_exercises_order
                                       UNIQUE (workout_id, display_order),

                                   CONSTRAINT ck_workout_exercises_order
                                       CHECK (display_order >= 0)
);


CREATE TABLE workout_sets (
                              id BIGSERIAL PRIMARY KEY,

                              workout_exercise_id BIGINT NOT NULL,

                              set_number INTEGER NOT NULL,

                              reps INTEGER,
                              weight DOUBLE PRECISION,
                              weight_unit VARCHAR(10),

                              duration_seconds BIGINT,
                              distance_meters DOUBLE PRECISION,

                              rpe DOUBLE PRECISION,

                              created_at TIMESTAMPTZ NOT NULL,

                              CONSTRAINT fk_workout_sets_workout_exercise
                                  FOREIGN KEY (workout_exercise_id)
                                      REFERENCES workout_exercises(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT uk_workout_sets_number
                                  UNIQUE (workout_exercise_id, set_number),

                              CONSTRAINT ck_workout_sets_number
                                  CHECK (set_number > 0),

                              CONSTRAINT ck_workout_sets_reps
                                  CHECK (reps IS NULL OR reps > 0),

                              CONSTRAINT ck_workout_sets_weight
                                  CHECK (weight IS NULL OR weight >= 0),

                              CONSTRAINT ck_workout_sets_duration
                                  CHECK (duration_seconds IS NULL OR duration_seconds >= 0),

                              CONSTRAINT ck_workout_sets_distance
                                  CHECK (distance_meters IS NULL OR distance_meters >= 0),

                              CONSTRAINT ck_workout_sets_rpe
                                  CHECK (rpe IS NULL OR (rpe >= 0 AND rpe <= 10))
);


CREATE INDEX idx_workouts_user_id
    ON workouts(user_id);

CREATE INDEX idx_workouts_started_at
    ON workouts(started_at);

CREATE INDEX idx_workouts_user_started_at
    ON workouts(user_id, started_at DESC);

CREATE INDEX idx_workout_exercises_workout_id
    ON workout_exercises(workout_id);

CREATE INDEX idx_workout_exercises_exercise_id
    ON workout_exercises(exercise_id);

CREATE INDEX idx_workout_sets_workout_exercise_id
    ON workout_sets(workout_exercise_id);