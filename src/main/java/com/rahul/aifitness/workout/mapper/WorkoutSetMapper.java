package com.rahul.aifitness.workout.mapper;

import com.rahul.aifitness.workout.dto.request.CreateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutSetResponse;
import com.rahul.aifitness.workout.entity.WorkoutExercise;
import com.rahul.aifitness.workout.entity.WorkoutSet;
import org.springframework.stereotype.Component;

@Component
public class WorkoutSetMapper {

    public WorkoutSet toEntity(
            CreateWorkoutSetRequest request,
            WorkoutExercise workoutExercise
    ) {
        return WorkoutSet.builder()
                .workoutExercise(workoutExercise)
                .setNumber(request.setNumber())
                .reps(request.reps())
                .weight(request.weight())
                .weightUnit(normalizeWeightUnit(request.weightUnit()))
                .durationSeconds(request.durationSeconds())
                .distanceMeters(request.distanceMeters())
                .rpe(request.rpe())
                .build();
    }

    public WorkoutSetResponse toResponse(
            WorkoutSet workoutSet
    ) {
        return new WorkoutSetResponse(
                workoutSet.getId(),
                workoutSet.getWorkoutExercise().getId(),
                workoutSet.getSetNumber(),
                workoutSet.getReps(),
                workoutSet.getWeight(),
                workoutSet.getWeightUnit(),
                workoutSet.getDurationSeconds(),
                workoutSet.getDistanceMeters(),
                workoutSet.getRpe(),
                workoutSet.getCreatedAt()
        );
    }

    public void updateEntity(
            WorkoutSet workoutSet,
            UpdateWorkoutSetRequest request
    ) {
        workoutSet.updateSet(
                request.setNumber(),
                request.reps(),
                request.weight(),
                normalizeWeightUnit(request.weightUnit()),
                request.durationSeconds(),
                request.distanceMeters(),
                request.rpe()
        );
    }

    private String normalizeWeightUnit(
            String weightUnit
    ) {
        if (weightUnit == null) {
            return null;
        }

        String normalized = weightUnit.trim();

        return normalized.isEmpty()
                ? null
                : normalized.toUpperCase();
    }
}