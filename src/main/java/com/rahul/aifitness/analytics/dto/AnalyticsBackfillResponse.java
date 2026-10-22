package com.rahul.aifitness.analytics.dto;

public record AnalyticsBackfillResponse(
        int totalWorkouts,
        int newlyAnalyzed,
        int alreadyAnalyzed
) {
}