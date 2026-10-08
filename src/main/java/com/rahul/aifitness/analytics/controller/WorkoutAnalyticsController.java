package com.rahul.aifitness.analytics.controller;

import com.rahul.aifitness.analytics.dto.WorkoutMetrics;
import com.rahul.aifitness.analytics.service.WorkoutAnalyticsService;
import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/workouts")
@RequiredArgsConstructor
public class WorkoutAnalyticsController {

    private final WorkoutAnalyticsService workoutAnalyticsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{workoutId}/analytics")
    public ResponseEntity<ApiResponse<WorkoutMetrics>> getWorkoutAnalytics(
            @PathVariable Long workoutId
    ) {

        Long userId = currentUserService.getCurrentUserId();

        WorkoutMetrics metrics =
                workoutAnalyticsService.getMetrics(workoutId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(metrics)
        );
    }

    @GetMapping("/analytics/summary")
    public ResponseEntity<ApiResponse<WorkoutAnalyticsSummary>> getAnalyticsSummary(
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {

        Long userId = currentUserService.getCurrentUserId();

        WorkoutAnalyticsSummary summary =
                workoutAnalyticsService.getSummary(
                        userId,
                        start,
                        end
                );

        return ResponseEntity.ok(
                ApiResponse.success(summary)
        );
    }
}