package com.rahul.aifitness.workout.repository;

import com.rahul.aifitness.workout.entity.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    List<WorkoutSet> findByWorkoutExerciseIdOrderBySetNumberAsc(
            Long workoutExerciseId
    );
}