package com.rahul.aifitness.ai.tool;

import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.ai.chat.model.ToolContext;
import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;
import com.rahul.aifitness.analytics.service.WorkoutAnalyticsService;
import com.rahul.aifitness.ai.dto.RecentWorkout;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.time.OffsetDateTime;

import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class AiFitnessTools {

    private final WorkoutRepository workoutRepository;
    private final WorkoutAnalyticsService workoutAnalyticsService;

    @Tool(
            name = "getLastWorkout",
            description = """
                    Get the user's most recent workout.
                    Use this when the user asks about their last workout,
                    previous workout, most recent workout, or what they
                    trained most recently.
                    """
    )
    public String getLastWorkout(ToolContext toolContext) {

        Object userIdValue =
                toolContext.getContext().get("userId");

        if (userIdValue == null) {
            return "Unable to identify the authenticated user.";
        }
        Long userId = Long.valueOf(userIdValue.toString());
        Optional<Workout> workout =
                workoutRepository.findTopByUserIdOrderByStartedAtDesc(userId);

        if (workout.isEmpty()) {
            return "No workouts found for this user.";
        }

        Workout w = workout.get();

        return """
                Last workout:
                Workout ID: %d
                Type: %s
                Started At: %s
                Duration Seconds: %s
                Distance Meters: %s
                Calories: %s
                Average Heart Rate: %s
                Maximum Heart Rate: %s
                Notes: %s
                """.formatted(
                w.getId(),
                w.getWorkoutType(),
                w.getStartedAt()
                        .atZoneSameInstant(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                w.getDurationSeconds(),
                w.getDistanceMeters(),
                w.getCalories(),
                w.getAverageHeartRate(),
                w.getMaxHeartRate(),
                w.getNotes()
        );
    }

    @Tool(
            name = "getWorkoutSummary",
            description = """
                Get the authenticated user's workout summary for a specified
                number of recent days.

                Use this when the user asks about:
                - recent training
                - weekly training
                - monthly training
                - total workouts
                - total distance
                - total duration
                - calories
                - workout type breakdown
                - overall recent activity
                """
    )
    public WorkoutAnalyticsSummary getWorkoutSummary(
            @ToolParam(
                    description = "Number of recent days to analyze. Use a value between 1 and 90."
            )
            Integer days,
            ToolContext toolContext
    ) {

        Object userIdValue =
                toolContext.getContext().get("userId");

        if (userIdValue == null) {
            throw new IllegalStateException(
                    "Unable to identify the authenticated user."
            );
        }

        Long userId = Long.valueOf(userIdValue.toString());

        if (days == null || days < 1 || days > 90) {
            throw new IllegalArgumentException(
                    "Days must be between 1 and 90."
            );
        }

        OffsetDateTime todayStart =
                OffsetDateTime.now(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.DAYS);

        OffsetDateTime start =
                todayStart.minusDays(days - 1L);

        OffsetDateTime end =
                todayStart
                        .plusDays(1)
                        .minusNanos(1);

        return workoutAnalyticsService.getSummary(
                userId,
                start,
                end
        );
    }

    @Tool(
            name = "getRecentWorkouts",
            description = """
                Get the authenticated user's most recent workouts.

                Use this when the user asks for:
                - last few workouts
                - recent workouts
                - recent training history
                - what workouts they did recently
                """
    )
    public List<RecentWorkout> getRecentWorkouts(
            @ToolParam(
                    description = "Number of recent workouts to retrieve. Must be between 1 and 10."
            )
            Integer limit,
            ToolContext toolContext
    ) {

        Object userIdValue =
                toolContext.getContext().get("userId");

        if (userIdValue == null) {
            throw new IllegalStateException(
                    "Unable to identify the authenticated user."
            );
        }

        Long userId = Long.valueOf(userIdValue.toString());

        if (limit == null || limit < 1 || limit > 10) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 10."
            );
        }

        List<Workout> workouts =
                workoutRepository.findByUserIdOrderByStartedAtDesc(
                        userId,
                        PageRequest.of(0, limit)
                );

        return workouts.stream()
                .map(workout -> new RecentWorkout(
                        workout.getId(),
                        workout.getWorkoutType(),
                        workout.getStartedAt(),
                        workout.getDurationSeconds(),
                        workout.getDistanceMeters(),
                        workout.getCalories(),
                        workout.getAverageHeartRate(),
                        workout.getMaxHeartRate()
                ))
                .toList();
    }
}