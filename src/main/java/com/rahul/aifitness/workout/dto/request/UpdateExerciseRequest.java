package com.rahul.aifitness.workout.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExerciseRequest(

        @NotBlank(message = "Exercise name is required")
        @Size(
                max = 150,
                message = "Exercise name must not exceed 150 characters"
        )
        String name
) {
}