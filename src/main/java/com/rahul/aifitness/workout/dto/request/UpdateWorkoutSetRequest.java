package com.rahul.aifitness.workout.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutSetRequest(

        @NotNull(message = "Set number is required")
        @Positive(message = "Set number must be positive")
        Integer setNumber,

        @Positive(message = "Reps must be positive")
        Integer reps,

        @PositiveOrZero(message = "Weight cannot be negative")
        Double weight,

        @Size(max = 10, message = "Weight unit must not exceed 10 characters")
        String weightUnit,

        @PositiveOrZero(message = "Duration cannot be negative")
        Integer durationSeconds,

        @PositiveOrZero(message = "Distance cannot be negative")
        Double distanceMeters,

        @DecimalMin(value = "0.0", message = "RPE cannot be negative")
        @DecimalMax(value = "10.0", message = "RPE cannot be greater than 10")
        Double rpe
) {
}