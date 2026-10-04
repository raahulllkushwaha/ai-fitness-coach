package com.rahul.aifitness.user.mapper;

import com.rahul.aifitness.user.dto.request.CreateUserRequest;
import com.rahul.aifitness.user.dto.request.UpdateUserRequest;
import com.rahul.aifitness.user.dto.response.UserResponse;
import com.rahul.aifitness.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        return User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .firstName(request.firstName().trim())
                .lastName(
                        request.lastName() == null
                                ? null
                                : request.lastName().trim()
                )
                .build();
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public void updateEntity(
            User user,
            UpdateUserRequest request
    ) {
        user.updateProfile(
                request.username().trim(),
                request.email().trim().toLowerCase(),
                request.firstName().trim(),
                request.lastName() == null
                        ? null
                        : request.lastName().trim()
        );
    }
}