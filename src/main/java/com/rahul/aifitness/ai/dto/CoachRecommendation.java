package com.rahul.aifitness.ai.dto;

import java.util.List;

public record CoachRecommendation(
        String overallAssessment,
        List<String> keyInsights,
        List<String> recommendations,
        String recoveryAdvice,
        String nextWorkoutSuggestion
) {
}