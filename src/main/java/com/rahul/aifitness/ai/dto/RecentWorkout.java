package com.rahul.aifitness.ai.dto;

import com.rahul.aifitness.workout.entity.WorkoutType;

import java.time.OffsetDateTime;

public record RecentWorkout(
        Long workoutId,
        WorkoutType workoutType,
        OffsetDateTime startedAt,
        Integer durationSeconds,
        Double distanceMeters,
        Double calories,
        Integer averageHeartRate,
        Integer maxHeartRate
) {
}