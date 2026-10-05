package com.rahul.aifitness.workout.dto.response;

import java.time.OffsetDateTime;

public record WorkoutSetResponse(
        Long id,
        Long workoutExerciseId,
        Integer setNumber,
        Integer reps,
        Double weight,
        String weightUnit,
        Integer durationSeconds,
        Double distanceMeters,
        Double rpe,
        OffsetDateTime createdAt
) {
}