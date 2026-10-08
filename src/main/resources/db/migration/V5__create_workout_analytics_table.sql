CREATE TABLE workout_analytics (
                                   id BIGSERIAL PRIMARY KEY,

                                   workout_id BIGINT NOT NULL,

                                   duration_seconds INTEGER,
                                   distance_meters DOUBLE PRECISION,

                                   average_speed_kmh DOUBLE PRECISION,
                                   average_pace_seconds_per_km DOUBLE PRECISION,

                                   calories DOUBLE PRECISION,
                                   average_heart_rate INTEGER,
                                   max_heart_rate INTEGER,

                                   elevation_gain_meters DOUBLE PRECISION,

                                   created_at TIMESTAMPTZ NOT NULL,
                                   updated_at TIMESTAMPTZ NOT NULL,

                                   CONSTRAINT fk_workout_analytics_workout
                                       FOREIGN KEY (workout_id)
                                           REFERENCES workouts(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT uk_workout_analytics_workout
                                       UNIQUE (workout_id)
);

CREATE INDEX idx_workout_analytics_workout_id
    ON workout_analytics(workout_id);