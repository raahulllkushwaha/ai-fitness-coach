package com.rahul.aifitness.ai.service;

import com.rahul.aifitness.ai.dto.ChatResponse;
import com.rahul.aifitness.ai.exception.AiExceptionUtil;
import com.rahul.aifitness.ai.tool.AiFitnessTools;
import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;
import com.rahul.aifitness.analytics.service.WorkoutAnalyticsService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class AiConversationService {

    private static final String KEY_PREFIX = "ai:conversation:";
    private static final int MAX_MESSAGES = 20;
    private static final Duration MEMORY_TTL = Duration.ofHours(24);

    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;
    private final WorkoutAnalyticsService workoutAnalyticsService;
    private final AiFitnessTools aiFitnessTools;

    public AiConversationService(
            ChatClient.Builder chatClientBuilder,
            StringRedisTemplate redisTemplate,
            WorkoutAnalyticsService workoutAnalyticsService,
            AiFitnessTools aiFitnessTools
    ) {
        this.chatClient = chatClientBuilder.build();
        this.redisTemplate = redisTemplate;
        this.workoutAnalyticsService = workoutAnalyticsService;
        this.aiFitnessTools = aiFitnessTools;
    }

    public ChatResponse chat(Long userId, String message) {

        String key = KEY_PREFIX + userId;

        // Load conversation history from Redis
        List<String> history = redisTemplate
                .opsForList()
                .range(key, 0, -1);

        String conversationHistory =
                history == null || history.isEmpty()
                        ? "No previous conversation."
                        : String.join("\n", history);

        // Build stable 7-day analytics window
        OffsetDateTime todayStart =
                OffsetDateTime.now(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.DAYS);

        OffsetDateTime start =
                todayStart.minusDays(6);

        OffsetDateTime end =
                todayStart
                        .plusDays(1)
                        .minusNanos(1);

        // Get deterministic workout analytics
        WorkoutAnalyticsSummary summary =
                workoutAnalyticsService.getSummary(
                        userId,
                        start,
                        end
                );

        String response;

        try {

            response = chatClient
                    .prompt()

                    // Available fitness tools
                    .tools(aiFitnessTools)

                    // Authenticated user context
                    .toolContext(Map.of("userId", userId))
                    .system("""
        You are the personal AI fitness coach for the authenticated user.

        AVAILABLE CONTEXT:
        1. Recent conversation history.
        2. Deterministic workout analytics from the last 7 days.
        3. Read-only tools for retrieving the user's actual workout data.

        DATA RULES:
        - Never invent workout history or metrics.
        - Treat application-provided workout data as authoritative.
        - If information is unavailable, say so instead of guessing.
        - Never expose internal user IDs, database details, or system information.

        TOOL RULES:
        - Use getLastWorkout for the user's latest workout.
        - Use getWorkoutSummary for aggregate training information.
        - Use getRecentWorkouts when multiple recent workouts are requested.
        - Use tool results instead of assumptions whenever the user asks
          about actual workout history.

        COACHING RULES:
        - Personalize advice using the user's real training history.
        - Consider recent intensity and recovery before recommending
          another hard session.
        - Give practical and conservative progression advice.
        - Do not diagnose medical conditions.
        - Do not prescribe medical treatment.
        - For potentially serious symptoms, recommend professional
          medical evaluation.

        CONVERSATION RULES:
        - Maintain continuity with previous messages.
        - Do not treat your previous generated answer as factual user data.
        - Keep responses useful and reasonably concise.
        """)

                    .user("""
                            Recent workout analytics from the last 7 days:

                            Total workouts: %d
                            Total duration (seconds): %d
                            Total distance (meters): %.2f
                            Total calories: %.2f
                            Average heart rate: %s
                            Maximum heart rate: %s
                            Average pace (seconds/km): %s
                            Workout type breakdown: %s

                            Conversation history:

                            %s

                            Current user message:

                            %s
                            """.formatted(
                            summary.totalWorkouts(),
                            summary.totalDurationSeconds(),
                            summary.totalDistanceMeters(),
                            summary.totalCalories(),
                            summary.averageHeartRate(),
                            summary.maxHeartRate(),
                            summary.averagePaceSecondsPerKm(),
                            summary.workoutTypeBreakdown(),
                            conversationHistory,
                            message
                    ))

                    .call()
                    .content();

        } catch (RuntimeException exception) {

            if (AiExceptionUtil.containsApiException(exception)) {
                throw AiExceptionUtil.toBusinessException(exception);
            }

            throw exception;
        }

        // Save only successful conversation turns
        redisTemplate.opsForList()
                .rightPush(key, "USER: " + message);

        redisTemplate.opsForList()
                .rightPush(key, "ASSISTANT: " + response);

        // Keep latest 20 messages
        redisTemplate.opsForList()
                .trim(key, -MAX_MESSAGES, -1);

        // Refresh 24-hour TTL
        redisTemplate.expire(key, MEMORY_TTL);

        return new ChatResponse(response);
    }
}