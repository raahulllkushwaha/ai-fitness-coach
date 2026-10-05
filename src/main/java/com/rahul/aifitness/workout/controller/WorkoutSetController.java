package com.rahul.aifitness.workout.controller;

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
        "/api/v1/users/{userId}/workouts/{workoutId}/exercises/{workoutExerciseId}/sets"
)
@RequiredArgsConstructor
public class WorkoutSetController {

    private final WorkoutSetService workoutSetService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkoutSetResponse>> addSetToWorkoutExercise(
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @Valid @RequestBody CreateWorkoutSetRequest request
    ) {
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
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId
    ) {
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
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId
    ) {
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
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId,
            @Valid @RequestBody UpdateWorkoutSetRequest request
    ) {
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
            @PathVariable Long userId,
            @PathVariable Long workoutId,
            @PathVariable Long workoutExerciseId,
            @PathVariable Long workoutSetId
    ) {
        workoutSetService.deleteWorkoutSet(
                userId,
                workoutId,
                workoutExerciseId,
                workoutSetId
        );

        return ResponseEntity.noContent().build();
    }
}