package com.rahul.aifitness.analytics.dto;

import com.rahul.aifitness.workout.entity.WorkoutType;

public record WorkoutMetrics(
        Long workoutId,
        WorkoutType workoutType,

        Integer durationSeconds,
        Double distanceMeters,

        Double averageSpeedKmh,
        Double averagePaceSecondsPerKm,

        Double calories,
        Integer averageHeartRate,
        Integer maxHeartRate,

        Double elevationGainMeters
) {
}