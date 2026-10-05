package com.rahul.aifitness.workout.controller;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutExerciseResponse;
import com.rahul.aifitness.workout.service.WorkoutExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workouts/{workoutId}/exercises")
@RequiredArgsConstructor
public class WorkoutExerciseController {

    private final WorkoutExerciseService workoutExerciseService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutExerciseResponse>> addExerciseToWorkout(
            @PathVariable Long workoutId,
            @Valid @RequestBody CreateWorkoutExerciseRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutExerciseResponse response =
                workoutExerciseService.addExerciseToWorkout(
                        userId,
                        workoutId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Exercise added to workout successfully",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutExerciseResponse>>> getWorkoutExercises(
            @PathVariable Long workoutId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        List<WorkoutExerciseResponse> response =
                workoutExerciseService.getWorkoutExercises(
                        userId,
                        workoutId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/{workoutExerciseId}")
    public ResponseEntity<ApiResponse<WorkoutExerciseResponse>> getWorkoutExerciseById(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutExerciseResponse response =
                workoutExerciseService.getWorkoutExerciseById(
                        userId,
                        workoutId,
                        workoutExerciseId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PutMapping("/{workoutExerciseId}")
    public ResponseEntity<ApiResponse<WorkoutExerciseResponse>> updateWorkoutExercise(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody UpdateWorkoutExerciseRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutExerciseResponse response =
                workoutExerciseService.updateWorkoutExercise(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workout exercise updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{workoutExerciseId}")
    public ResponseEntity<Void> removeExerciseFromWorkout(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        workoutExerciseService.removeExerciseFromWorkout(
                userId,
                workoutId,
                workoutExerciseId
        );

        return ResponseEntity.noContent().build();
    }
}