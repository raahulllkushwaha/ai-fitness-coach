package com.rahul.aifitness.analytics.dto;

import com.rahul.aifitness.workout.entity.WorkoutType;

import java.util.Map;

public record WorkoutAnalyticsSummary(
        Integer totalWorkouts,
        Long totalDurationSeconds,
        Double totalDistanceMeters,
        Double totalCalories,
        Double averageHeartRate,
        Integer maxHeartRate,
        Double averagePaceSecondsPerKm,
        Map<WorkoutType, Long> workoutTypeBreakdown
) {
}