package com.rahul.aifitness.analytics.service;

import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;
import com.rahul.aifitness.analytics.dto.WorkoutMetrics;
import com.rahul.aifitness.analytics.entity.WorkoutAnalytics;
import com.rahul.aifitness.analytics.repository.WorkoutAnalyticsRepository;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.workout.entity.WorkoutType;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutAnalyticsService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutAnalyticsRepository workoutAnalyticsRepository;

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "analyticsSummary",
                    allEntries = true
            ),
            @CacheEvict(
                    cacheNames = "aiCoach",
                    allEntries = true
            )
    })
    @Transactional
    public WorkoutMetrics analyzeWorkout(Long workoutId) {

        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workout not found with id: " + workoutId
                        )
                );

        Integer durationSeconds = workout.getDurationSeconds();
        Double distanceMeters = workout.getDistanceMeters();

        Double averageSpeedKmh =
                calculateAverageSpeed(durationSeconds, distanceMeters);

        Double averagePaceSecondsPerKm =
                calculateAveragePace(durationSeconds, distanceMeters);

        WorkoutAnalytics analytics =
                workoutAnalyticsRepository.findByWorkoutId(workoutId)
                        .orElseGet(() -> WorkoutAnalytics.builder()
                                .workout(workout)
                                .build());

        analytics.updateMetrics(
                durationSeconds,
                distanceMeters,
                averageSpeedKmh,
                averagePaceSecondsPerKm,
                workout.getCalories(),
                workout.getAverageHeartRate(),
                workout.getMaxHeartRate(),
                workout.getElevationGainMeters()
        );

        workoutAnalyticsRepository.save(analytics);

        return new WorkoutMetrics(
                workout.getId(),
                workout.getWorkoutType(),
                durationSeconds,
                distanceMeters,
                averageSpeedKmh,
                averagePaceSecondsPerKm,
                workout.getCalories(),
                workout.getAverageHeartRate(),
                workout.getMaxHeartRate(),
                workout.getElevationGainMeters()
        );
    }

    @Transactional(readOnly = true)
    public WorkoutMetrics getMetrics(Long workoutId, Long userId) {

        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workout not found with id: " + workoutId
                        )
                );

        WorkoutAnalytics analytics =
                workoutAnalyticsRepository.findByWorkoutId(workoutId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Analytics not found for workout id: " + workoutId
                                )
                        );

        return toMetrics(analytics);
    }

    @Cacheable(
            cacheNames = "analyticsSummary",
            key = "#userId + ':' + #start.toEpochSecond() + ':' + #end.toEpochSecond()"
    )
    @Transactional(readOnly = true)
    public WorkoutAnalyticsSummary getSummary(
            Long userId,
            OffsetDateTime start,
            OffsetDateTime end
    ) {

        if (start == null || end == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Start and end dates are required",
                    "Invalid Date Range"
            );
        }

        if (start.isAfter(end)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Start date must be before or equal to end date",
                    "Invalid Date Range"
            );
        }

        List<WorkoutAnalytics> analyticsList =
                workoutAnalyticsRepository.findByUserIdAndDateRange(
                        userId,
                        start,
                        end
                );

        if (analyticsList.isEmpty()) {
            return new WorkoutAnalyticsSummary(
                    0,
                    0L,
                    0.0,
                    0.0,
                    null,
                    null,
                    null,
                    Map.of()
            );
        }

        int totalWorkouts = analyticsList.size();

        long totalDurationSeconds = analyticsList.stream()
                .map(WorkoutAnalytics::getDurationSeconds)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();

        double totalDistanceMeters = analyticsList.stream()
                .map(WorkoutAnalytics::getDistanceMeters)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        double totalCalories = analyticsList.stream()
                .map(WorkoutAnalytics::getCalories)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        Double averageHeartRate = analyticsList.stream()
                .map(WorkoutAnalytics::getAverageHeartRate)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);

        if (Double.isNaN(averageHeartRate)) {
            averageHeartRate = null;
        } else {
            averageHeartRate = round(averageHeartRate);
        }

        Integer maxHeartRate = analyticsList.stream()
                .map(WorkoutAnalytics::getMaxHeartRate)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(null);

        Double averagePace = analyticsList.stream()
                .map(WorkoutAnalytics::getAveragePaceSecondsPerKm)
                .filter(value -> value != null && value > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);

        if (Double.isNaN(averagePace)) {
            averagePace = null;
        } else {
            averagePace = round(averagePace);
        }

        Map<WorkoutType, Long> workoutTypeBreakdown =
                new EnumMap<>(WorkoutType.class);

        analyticsList.stream()
                .map(WorkoutAnalytics::getWorkout)
                .map(workout -> workout.getWorkoutType())
                .forEach(type ->
                        workoutTypeBreakdown.merge(type, 1L, Long::sum)
                );

        return new WorkoutAnalyticsSummary(
                totalWorkouts,
                totalDurationSeconds,
                round(totalDistanceMeters),
                round(totalCalories),
                averageHeartRate,
                maxHeartRate,
                averagePace,
                workoutTypeBreakdown
        );
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "analyticsSummary",
                    allEntries = true
            ),
            @CacheEvict(
                    cacheNames = "aiCoach",
                    allEntries = true
            )
    })
    @Transactional
    public void deleteAnalytics(Long workoutId) {
        workoutAnalyticsRepository.deleteByWorkoutId(workoutId);
    }

    private Double calculateAverageSpeed(
            Integer durationSeconds,
            Double distanceMeters
    ) {

        if (durationSeconds == null ||
                durationSeconds <= 0 ||
                distanceMeters == null ||
                distanceMeters <= 0) {

            return null;
        }

        double distanceKm = distanceMeters / 1000.0;
        double durationHours = durationSeconds / 3600.0;

        return round(distanceKm / durationHours);
    }

    private Double calculateAveragePace(
            Integer durationSeconds,
            Double distanceMeters
    ) {

        if (durationSeconds == null ||
                durationSeconds <= 0 ||
                distanceMeters == null ||
                distanceMeters <= 0) {

            return null;
        }

        double distanceKm = distanceMeters / 1000.0;

        return round(durationSeconds / distanceKm);
    }

    private WorkoutMetrics toMetrics(WorkoutAnalytics analytics) {

        Workout workout = analytics.getWorkout();

        return new WorkoutMetrics(
                workout.getId(),
                workout.getWorkoutType(),
                analytics.getDurationSeconds(),
                analytics.getDistanceMeters(),
                analytics.getAverageSpeedKmh(),
                analytics.getAveragePaceSecondsPerKm(),
                analytics.getCalories(),
                analytics.getAverageHeartRate(),
                analytics.getMaxHeartRate(),
                analytics.getElevationGainMeters()
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}