package com.rahul.aifitness.workout.dto.response;

import java.time.OffsetDateTime;

public record WorkoutExerciseResponse(
        Long id,
        Long workoutId,
        Long exerciseId,
        String exerciseName,
        Integer displayOrder,
        OffsetDateTime createdAt
) {
}