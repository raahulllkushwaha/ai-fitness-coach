package com.rahul.aifitness.workout.entity;

import com.rahul.aifitness.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "workouts",
        indexes = {
                @Index(name = "idx_workouts_user_id", columnList = "user_id"),
                @Index(name = "idx_workouts_started_at", columnList = "started_at"),
                @Index(
                        name = "idx_workouts_user_started_at",
                        columnList = "user_id, started_at"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "workout_type", nullable = false, length = 30)
    private WorkoutType workoutType;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "average_pace_seconds_per_km")
    private Double averagePaceSecondsPerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private WorkoutSource source;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateWorkout(
            WorkoutType workoutType,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer durationSeconds,
            Double distanceMeters,
            Double elevationGainMeters,
            Integer averageHeartRate,
            Integer maxHeartRate,
            Double calories,
            Double averagePaceSecondsPerKm,
            WorkoutSource source,
            String externalId,
            String notes
    ) {
        this.workoutType = workoutType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.elevationGainMeters = elevationGainMeters;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.calories = calories;
        this.averagePaceSecondsPerKm = averagePaceSecondsPerKm;
        this.source = source;
        this.externalId = externalId;
        this.notes = notes;
    }
}