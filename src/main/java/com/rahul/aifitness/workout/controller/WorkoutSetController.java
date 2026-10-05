package com.rahul.aifitness.workout.controller;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutSetResponse;
import com.rahul.aifitness.workout.service.WorkoutSetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/v1/workouts/{workoutId}/exercises/{workoutExerciseId}/sets"
)
@RequiredArgsConstructor
public class WorkoutSetController {

    private final WorkoutSetService workoutSetService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutSetResponse>> addSetToWorkoutExercise(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody CreateWorkoutSetRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutSetResponse response =
                workoutSetService.addSetToWorkoutExercise(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Workout set added successfully",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutSetResponse>>> getWorkoutSets(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        List<WorkoutSetResponse> response =
                workoutSetService.getWorkoutSets(
                        userId,
                        workoutId,
                        workoutExerciseId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping("/{workoutSetId}")
    public ResponseEntity<ApiResponse<WorkoutSetResponse>> getWorkoutSetById(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutSetResponse response =
                workoutSetService.getWorkoutSetById(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        workoutSetId
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PutMapping("/{workoutSetId}")
    public ResponseEntity<ApiResponse<WorkoutSetResponse>> updateWorkoutSet(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId,
            @Valid @RequestBody UpdateWorkoutSetRequest request
    ) {
        Long userId = currentUserService.getCurrentUserId();

        WorkoutSetResponse response =
                workoutSetService.updateWorkoutSet(
                        userId,
                        workoutId,
                        workoutExerciseId,
                        workoutSetId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workout set updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{workoutSetId}")
    public ResponseEntity<Void> deleteWorkoutSet(
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId
    ) {
        Long userId = currentUserService.getCurrentUserId();

        workoutSetService.deleteWorkoutSet(
                userId,
                workoutId,
                workoutExerciseId,
                workoutSetId
        );

        return ResponseEntity.noContent().build();
    }
}