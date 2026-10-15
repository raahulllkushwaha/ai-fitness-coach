CREATE UNIQUE INDEX uq_workouts_user_external_id
    ON workouts (user_id, external_id)
    WHERE external_id IS NOT NULL;