package com.rahul.aifitness.workout.dto.request;

import com.rahul.aifitness.workout.entity.WorkoutSource;
import com.rahul.aifitness.workout.entity.WorkoutType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;

public record CreateWorkoutRequest(

        @NotNull(message = "Workout type is required")
        WorkoutType workoutType,

        @NotNull(message = "Workout start time is required")
        OffsetDateTime startedAt,

        OffsetDateTime endedAt,

        @PositiveOrZero(message = "Duration cannot be negative")
        Integer durationSeconds,

        @PositiveOrZero(message = "Distance cannot be negative")
        Double distanceMeters,

        @PositiveOrZero(message = "Elevation gain cannot be negative")
        Double elevationGainMeters,

        @PositiveOrZero(message = "Average heart rate cannot be negative")
        Integer averageHeartRate,

        @PositiveOrZero(message = "Maximum heart rate cannot be negative")
        Integer maxHeartRate,

        @PositiveOrZero(message = "Calories cannot be negative")
        Double calories,

        @PositiveOrZero(message = "Average pace cannot be negative")
        Double averagePaceSecondsPerKm,

        @NotNull(message = "Workout source is required")
        WorkoutSource source,

        String externalId,

        String notes
) {
}