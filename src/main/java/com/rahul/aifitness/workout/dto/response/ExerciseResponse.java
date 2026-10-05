package com.rahul.aifitness.workout.dto.response;

import java.time.OffsetDateTime;

public record ExerciseResponse(
        Long id,
        String name,
        String normalizedName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}