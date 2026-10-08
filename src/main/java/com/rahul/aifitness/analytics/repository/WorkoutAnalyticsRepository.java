package com.rahul.aifitness.analytics.repository;

import com.rahul.aifitness.analytics.entity.WorkoutAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkoutAnalyticsRepository
        extends JpaRepository<WorkoutAnalytics, Long> {

    Optional<WorkoutAnalytics> findByWorkoutId(Long workoutId);

    boolean existsByWorkoutId(Long workoutId);

    void deleteByWorkoutId(Long workoutId);

    @Query("""
            SELECT wa
            FROM WorkoutAnalytics wa
            JOIN FETCH wa.workout w
            WHERE w.user.id = :userId
              AND w.startedAt >= :start
              AND w.startedAt <= :end
            ORDER BY w.startedAt
            """)
    List<WorkoutAnalytics> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}