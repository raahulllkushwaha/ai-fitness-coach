package com.rahul.aifitness.ai.service;

import com.rahul.aifitness.ai.dto.CoachRecommendation;
import com.rahul.aifitness.ai.exception.AiExceptionUtil;
import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;
import com.rahul.aifitness.analytics.service.WorkoutAnalyticsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AiCoachService {

    private final ChatClient chatClient;
    private final WorkoutAnalyticsService workoutAnalyticsService;

    public AiCoachService(
            ChatClient.Builder chatClientBuilder,
            WorkoutAnalyticsService workoutAnalyticsService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.workoutAnalyticsService = workoutAnalyticsService;
    }

    @Cacheable(
            cacheNames = "aiCoach",
            key = "#userId + ':' + #start.toEpochSecond() + ':' + #end.toEpochSecond()"
    )
    public CoachRecommendation generateRecommendation(
            Long userId,
            OffsetDateTime start,
            OffsetDateTime end
    ) {

        WorkoutAnalyticsSummary summary =
                workoutAnalyticsService.getSummary(
                        userId,
                        start,
                        end
                );

        try {

            return chatClient
                    .prompt()
                    .system("""
        You are an AI fitness coach for the AI Fitness Coach application.

        Your role is to help users understand their training,
        recovery, consistency, and workout planning.

        DATA RULES:
        - Use only the workout analytics provided by the application.
        - Never invent workouts, metrics, dates, heart rates, distances,
          calories, or training history.
        - If required information is unavailable, clearly say that it
          is unavailable.
        - Never claim that you accessed data that was not provided.

        SAFETY RULES:
        - Do not diagnose medical conditions.
        - Do not prescribe medication or medical treatment.
        - Do not present fitness guidance as medical advice.
        - For potentially serious symptoms, recommend appropriate
          professional medical evaluation.

        COACHING RULES:
        - Prefer practical, achievable recommendations.
        - Consider training load, consistency, intensity, and recovery.
        - Avoid unnecessarily aggressive progression.
        - Clearly distinguish facts from recommendations.
        - Keep recommendations concise and actionable.

        OUTPUT RULES:
        - Follow the requested response structure exactly.
        - Do not add fields outside the defined response schema.
        """)
                    .user("""
                            Analyze this workout summary:

                            Total workouts: %d
                            Total duration (seconds): %d
                            Total distance (meters): %.2f
                            Total calories: %.2f
                            Average heart rate: %s
                            Maximum heart rate: %s
                            Average pace (seconds/km): %s
                            Workout type breakdown: %s

                            Provide:
                            1. Overall assessment
                            2. Key insights
                            3. Practical recommendations
                            4. Recovery advice
                            5. Next workout suggestion
                            """
                            .formatted(
                                    summary.totalWorkouts(),
                                    summary.totalDurationSeconds(),
                                    summary.totalDistanceMeters(),
                                    summary.totalCalories(),
                                    summary.averageHeartRate(),
                                    summary.maxHeartRate(),
                                    summary.averagePaceSecondsPerKm(),
                                    summary.workoutTypeBreakdown()
                            ))
                    .call()
                    .entity(
                            CoachRecommendation.class,
                            spec -> spec
                                    .useProviderStructuredOutput()
                                    .validateSchema()
                    );

        } catch (RuntimeException exception) {

            if (AiExceptionUtil.containsApiException(exception)) {
                throw AiExceptionUtil.toBusinessException(exception);
            }

            throw exception;
        }
    }
}