package com.rahul.aifitness.auth.dto.response;

import com.rahul.aifitness.user.entity.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String username,
        UserRole role
) {
}