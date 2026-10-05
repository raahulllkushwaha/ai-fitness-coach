package com.rahul.aifitness.workout.repository;

import com.rahul.aifitness.workout.entity.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {

    List<WorkoutExercise> findByWorkoutIdOrderByDisplayOrderAsc(Long workoutId);

    boolean existsByWorkoutIdAndExerciseId(Long workoutId, Long exerciseId);
    boolean existsByExerciseId(Long exerciseId);
}