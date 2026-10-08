package com.rahul.aifitness.workout.repository;

import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.entity.WorkoutType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    Page<Workout> findByUserId(Long userId, Pageable pageable);

    Page<Workout> findByUserIdAndWorkoutType(
            Long userId,
            WorkoutType workoutType,
            Pageable pageable
    );

    Optional<Workout> findByIdAndUserId(Long workoutId, Long userId);

    Page<Workout> findByUserIdAndStartedAtBetween(
            Long userId,
            OffsetDateTime start,
            OffsetDateTime end,
            Pageable pageable
    );

    boolean existsByUserIdAndExternalId(
            Long userId,
            String externalId
    );

    Optional<Workout> findTopByUserIdOrderByStartedAtDesc(Long userId);

    List<Workout> findByUserIdOrderByStartedAtDesc(
            Long userId,
            Pageable pageable
    );
}