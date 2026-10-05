package com.rahul.aifitness.user.controller;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.user.dto.request.CreateUserRequest;
import com.rahul.aifitness.user.dto.request.UpdateUserRequest;
import com.rahul.aifitness.user.dto.response.UserResponse;
import com.rahul.aifitness.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User created successfully",
                                response
                        )
                );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        Long userId = currentUserService.getCurrentUserId();

        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current user fetched successfully",
                        response
                )
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();

        UserResponse response = userService.updateUser(
                userId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current user updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser() {

        Long userId = currentUserService.getCurrentUserId();

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long userId
    ) {
        UserResponse response = userService.getUserById(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User fetched successfully",
                        response
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Users fetched successfully",
                        users
                )
        );
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = userService.updateUser(
                userId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "User updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}