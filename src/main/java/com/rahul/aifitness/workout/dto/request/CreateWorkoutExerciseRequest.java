package com.rahul.aifitness.workout.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateWorkoutExerciseRequest(

        @NotNull(message = "Exercise id is required")
        @Positive(message = "Exercise id must be positive")
        Long exerciseId,

        @NotNull(message = "Display order is required")
        @Positive(message = "Display order must be positive")
        Integer displayOrder
) {
}