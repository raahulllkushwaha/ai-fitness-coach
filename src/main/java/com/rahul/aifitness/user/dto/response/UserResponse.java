package com.rahul.aifitness.user.dto.response;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}