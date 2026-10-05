package com.rahul.aifitness.workout.mapper;

import com.rahul.aifitness.workout.dto.request.CreateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutExerciseResponse;
import com.rahul.aifitness.workout.entity.Exercise;
import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.entity.WorkoutExercise;
import org.springframework.stereotype.Component;

@Component
public class WorkoutExerciseMapper {

    public WorkoutExercise toEntity(
            CreateWorkoutExerciseRequest request,
            Workout workout,
            Exercise exercise
    ) {
        return WorkoutExercise.builder()
                .workout(workout)
                .exercise(exercise)
                .displayOrder(request.displayOrder())
                .build();
    }

    public WorkoutExerciseResponse toResponse(
            WorkoutExercise workoutExercise
    ) {
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                workoutExercise.getWorkout().getId(),
                workoutExercise.getExercise().getId(),
                workoutExercise.getExercise().getName(),
                workoutExercise.getDisplayOrder(),
                workoutExercise.getCreatedAt()
        );
    }

    public void updateEntity(
            WorkoutExercise workoutExercise,
            UpdateWorkoutExerciseRequest request
    ) {
        workoutExercise.updateDisplayOrder(
                request.displayOrder()
        );
    }
}