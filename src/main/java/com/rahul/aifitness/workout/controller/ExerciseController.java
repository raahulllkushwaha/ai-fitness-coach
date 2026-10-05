package com.rahul.aifitness.workout.controller;

import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.workout.dto.request.CreateExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateExerciseRequest;
import com.rahul.aifitness.workout.dto.response.ExerciseResponse;
import com.rahul.aifitness.workout.service.ExerciseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExerciseResponse>> createExercise(
            @Valid @RequestBody CreateExerciseRequest request
    ) {
        ExerciseResponse response = exerciseService.createExercise(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exercise created successfully", response));
    }

    @GetMapping("/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseResponse>> getExerciseById(
            @PathVariable Long exerciseId
    ) {
        ExerciseResponse response = exerciseService.getExerciseById(exerciseId);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExerciseResponse>>> getAllExercises(
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable) {
        Page<ExerciseResponse> response =
                exerciseService.getAllExercises(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @PutMapping("/{exerciseId}")
    public ResponseEntity<ApiResponse<ExerciseResponse>> updateExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody UpdateExerciseRequest request
    ) {
        ExerciseResponse response = exerciseService.updateExercise(
                exerciseId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Exercise updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            @PathVariable Long exerciseId
    ) {
        exerciseService.deleteExercise(exerciseId);

        return ResponseEntity.noContent().build();
    }
}