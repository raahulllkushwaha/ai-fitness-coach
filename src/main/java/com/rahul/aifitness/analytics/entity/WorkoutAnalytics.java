package com.rahul.aifitness.analytics.entity;

import com.rahul.aifitness.workout.entity.Workout;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "workout_analytics",
        indexes = {
                @Index(
                        name = "idx_workout_analytics_workout_id",
                        columnList = "workout_id"
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkoutAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workout_id",
            nullable = false,
            unique = true
    )
    private Workout workout;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Double distanceMeters;

    @Column(name = "average_speed_kmh")
    private Double averageSpeedKmh;

    @Column(name = "average_pace_seconds_per_km")
    private Double averagePaceSecondsPerKm;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void updateMetrics(
            Integer durationSeconds,
            Double distanceMeters,
            Double averageSpeedKmh,
            Double averagePaceSecondsPerKm,
            Double calories,
            Integer averageHeartRate,
            Integer maxHeartRate,
            Double elevationGainMeters
    ) {
        this.durationSeconds = durationSeconds;
        this.distanceMeters = distanceMeters;
        this.averageSpeedKmh = averageSpeedKmh;
        this.averagePaceSecondsPerKm = averagePaceSecondsPerKm;
        this.calories = calories;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.elevationGainMeters = elevationGainMeters;
    }
}