package com.rahul.aifitness.workout.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "workout_sets",
        indexes = {
                @Index(
                        name = "idx_workout_sets_workout_exercise_id",
                        columnList = "workout_exercise_id"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workout_exercise_id",
            nullable = false
    )
    private WorkoutExercise workoutExercise;

    @Column(name = "set_number", nullable = false)
    private Integer setNumber;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "weight_unit", length = 10)
    private String weightUnit;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "rpe")
    private Double rpe;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}