package com.rahul.aifitness.workout.controller;

import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutResponse;
import com.rahul.aifitness.workout.entity.WorkoutType;
import com.rahul.aifitness.workout.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/users/{userId}/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutResponse>> createWorkout(
            @PathVariable Long userId,
            @Valid @RequestBody CreateWorkoutRequest request
    ) {
        WorkoutResponse response = workoutService.createWorkout(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Workout created successfully",
                                response
                        )
                );
    }

    @GetMapping("/{workoutId}")
    public ResponseEntity<ApiResponse<WorkoutResponse>> getWorkoutById(
            @PathVariable Long userId,
            @PathVariable Long workoutId
    ) {
        WorkoutResponse response = workoutService.getWorkoutById(
                userId,
                workoutId
        );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<WorkoutResponse>>> getAllWorkouts(
            @PathVariable Long userId,
            @PageableDefault(
                    size = 20,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<WorkoutResponse> response = workoutService.getAllWorkouts(
                userId,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/type/{workoutType}")
    public ResponseEntity<ApiResponse<Page<WorkoutResponse>>> getWorkoutsByType(
            @PathVariable Long userId,
            @PathVariable WorkoutType workoutType,
            @PageableDefault(
                    size = 20,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<WorkoutResponse> response =
                workoutService.getWorkoutsByType(
                        userId,
                        workoutType,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse<Page<WorkoutResponse>>> getWorkoutsBetween(
            @PathVariable Long userId,
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end,
            @PageableDefault(
                    size = 20,
                    sort = "startedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Page<WorkoutResponse> response =
                workoutService.getWorkoutsBetween(
                        userId,
                        start,
                        end,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PutMapping("/{workoutId}")
    public ResponseEntity<ApiResponse<WorkoutResponse>> updateWorkout(
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @Valid @RequestBody UpdateWorkoutRequest request
    ) {
        WorkoutResponse response = workoutService.updateWorkout(
                userId,
                workoutId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workout updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{workoutId}")
    public ResponseEntity<Void> deleteWorkout(
            @PathVariable Long userId,
            @PathVariable Long workoutId
    ) {
        workoutService.deleteWorkout(
                userId,
                workoutId
        );

        return ResponseEntity.noContent().build();
    }
}