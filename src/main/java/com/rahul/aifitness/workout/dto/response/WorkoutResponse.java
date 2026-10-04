package com.rahul.aifitness.workout.dto.response;

import com.rahul.aifitness.workout.entity.WorkoutSource;
import com.rahul.aifitness.workout.entity.WorkoutType;

import java.time.OffsetDateTime;

public record WorkoutResponse(
        Long id,
        Long userId,
        WorkoutType workoutType,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Integer durationSeconds,
        Double distanceMeters,
        Double elevationGainMeters,
        Integer averageHeartRate,
        Integer maxHeartRate,
        Double calories,
        Double averagePaceSecondsPerKm,
        WorkoutSource source,
        String externalId,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}