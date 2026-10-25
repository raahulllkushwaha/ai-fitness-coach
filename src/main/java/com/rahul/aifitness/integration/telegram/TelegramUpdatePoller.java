package com.rahul.aifitness.integration.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.rahul.aifitness.analytics.dto.WorkoutAnalyticsSummary;
import com.rahul.aifitness.analytics.service.WorkoutAnalyticsService;
import com.rahul.aifitness.integration.telegram.entity.TelegramAccountLink;
import com.rahul.aifitness.integration.telegram.repository.TelegramAccountLinkRepository;
import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.entity.WorkoutType;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.rahul.aifitness.integration.intervalsicu.IntervalsIcuPlannedWorkoutService;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsCalendarEventDto;
import java.time.LocalDateTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramUpdatePoller {

    private static final String OFFSET_KEY =
            "telegram:poller:next-offset";

    private static final String PROCESSED_PREFIX =
            "telegram:processed-update:";

    private static final Duration UPDATE_TTL =
            Duration.ofDays(7);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, HH:mm 'UTC'",
                    Locale.ENGLISH
            );

    private final TelegramBotClient telegramBotClient;
    private final TelegramLinkService telegramLinkService;
    private final TelegramAiMessageService telegramAiMessageService;
    private final TelegramAccountLinkRepository accountLinkRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutAnalyticsService workoutAnalyticsService;
    private final StringRedisTemplate redisTemplate;
    private final IntervalsIcuPlannedWorkoutService plannedWorkoutService;

    @Scheduled(fixedDelay = 1000)
    public void pollUpdates() {
        if (!telegramBotClient.isConfigured()) {
            return;
        }

        try {
            List<JsonNode> updates =
                    telegramBotClient.getUpdates(loadOffset());

            for (JsonNode update : updates) {
                long updateId =
                        update.path("update_id").asLong(-1);

                if (updateId < 0) {
                    continue;
                }

                String processedKey =
                        PROCESSED_PREFIX + updateId;

                try {
                    if (Boolean.TRUE.equals(
                            redisTemplate.hasKey(processedKey))) {
                        saveOffset(updateId + 1);
                        continue;
                    }

                    handleUpdate(update);

                    redisTemplate.opsForValue().set(
                            processedKey,
                            "1",
                            UPDATE_TTL
                    );

                    saveOffset(updateId + 1);

                    log.info(
                            "Telegram update processed: updateId={}",
                            updateId
                    );

                } catch (Exception exception) {
                    log.error(
                            "Telegram update processing failed: updateId={}, errorType={}",
                            updateId,
                            exception.getClass().getSimpleName()
                    );
                    break;
                }
            }

        } catch (Exception exception) {
            log.error(
                    "Telegram polling failed: errorType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private long loadOffset() {
        String value = redisTemplate.opsForValue().get(OFFSET_KEY);

        if (value == null || value.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            log.warn("Invalid Telegram polling offset in Redis");
            return 0L;
        }
    }

    private void saveOffset(long offset) {
        redisTemplate.opsForValue().set(
                OFFSET_KEY,
                Long.toString(offset)
        );
    }

    private void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");

        if (!message.isObject()) {
            return;
        }

        JsonNode chat = message.path("chat");
        JsonNode sender = message.path("from");
        JsonNode textNode = message.path("text");

        if (!chat.hasNonNull("id")
                || !sender.hasNonNull("id")
                || !textNode.isTextual()) {
            return;
        }

        // Commands and account data are handled in private chats only.
        if (!"private".equals(chat.path("type").asText())) {
            return;
        }

        long chatId = chat.path("id").asLong();
        long telegramUserId = sender.path("id").asLong();

        String username = sender.path("username").asText(null);
        String text = textNode.asText().trim();

        if (text.isBlank()) {
            return;
        }

        String[] parts = text.split("\\s+", 2);

        String command = parts[0]
                .split("@", 2)[0]
                .toLowerCase(Locale.ROOT);

        String argument = parts.length > 1
                ? parts[1].trim()
                : "";

        switch (command) {
            case "/start" -> telegramBotClient.sendMessage(
                    chatId,
                    """
                    💪 Welcome to AI Fitness Coach!

                    Your personal workout companion.

                    /help - Show all commands
                    /status - Check account connection
                    /lastworkout - View your latest workout
                    /recent - View your last 5 workouts
                    /summary - Last 7 days of fitness stats
                    /upcoming - Show planned workouts for the next 7 days.
                    /link CODE - Link your account

                    Send a normal message to chat with your AI Coach.
                    """
            );

            case "/help" -> telegramBotClient.sendMessage(
                    chatId,
                    """
                    🏋️ AI Fitness Coach — Commands

                    /start
                    Start the bot.

                    /status
                    Check if your account is linked.

                    /lastworkout
                    Show your latest workout.

                    /recent
                    Show your last 5 workouts.

                    /summary
                    Show your last 7 days of statistics.

                    /stats
                    Same as /summary.

                    /upcoming - View your planned workouts for the next 7 days

                    /link CODE
                    Link your AI Fitness Coach account.

                    You can also send a normal message to chat with your AI Coach.
                    """
            );

            case "/status" -> sendStatus(chatId, telegramUserId);

            case "/lastworkout", "/last" ->
                    sendLastWorkout(chatId, telegramUserId);

            case "/recent", "/workouts" ->
                    sendRecentWorkouts(chatId, telegramUserId);

            case "/summary", "/stats" ->
                    sendSummary(chatId, telegramUserId, argument);

            case "/upcoming" -> sendUpcomingWorkouts(chatId, telegramUserId);

            case "/link" ->
                    handleLinkCommand(
                            chatId,
                            telegramUserId,
                            username,
                            argument
                    );

            default -> {
                if (command.startsWith("/")) {
                    telegramBotClient.sendMessage(
                            chatId,
                            "Unknown command. Use /help to see available commands."
                    );
                } else {
                    // AI executes asynchronously; polling stays responsive.
                    telegramAiMessageService.processMessage(
                            chatId,
                            telegramUserId,
                            text
                    );
                }
            }
        }
    }

    private Optional<Long> findLinkedUserId(long telegramUserId) {
        return accountLinkRepository
                .findByTelegramUserId(telegramUserId)
                .map(TelegramAccountLink::getUserId);
    }

    private void sendStatus(long chatId, long telegramUserId) {
        Optional<Long> userId = findLinkedUserId(telegramUserId);

        if (userId.isEmpty()) {
            telegramBotClient.sendMessage(
                    chatId,
                    "⚠️ Your account is not linked yet. Use /link CODE."
            );
            return;
        }

        telegramBotClient.sendMessage(
                chatId,
                """
                ✅ Account connected!

                Your Telegram account is linked to AI Fitness Coach.
                You can use /lastworkout, /recent and /summary.
                """
        );
    }
    private void sendUpcomingWorkouts(
            long chatId,
            long telegramUserId
    ) {
        Optional<Long> userId = findLinkedUserId(telegramUserId);

        if (userId.isEmpty()) {
            sendNotLinkedMessage(chatId);
            return;
        }

        LocalDate today = LocalDate.now();

        try {
            List<IntervalsCalendarEventDto> events =
                    plannedWorkoutService.getPlannedWorkouts(
                            userId.get(),
                            today,
                            today.plusDays(7),
                            50
                    );

            if (events.isEmpty()) {
                telegramBotClient.sendMessage(
                        chatId,
                        "📅 No planned workouts found for the next 7 days.\n\n"
                                + "Check your Intervals.icu calendar or reconnect "
                                + "with calendar read permission."
                );
                return;
            }

            StringBuilder message = new StringBuilder(
                    "📅 Your Upcoming Workouts\n\n"
            );

            for (IntervalsCalendarEventDto event : events) {
                String name = event.name() == null || event.name().isBlank()
                        ? "Planned workout"
                        : event.name();

                message.append("🏋️ ")
                        .append(name)
                        .append("\n");

                if (event.type() != null && !event.type().isBlank()) {
                    message.append("Type: ")
                            .append(event.type())
                            .append("\n");
                }

                message.append("When: ")
                        .append(formatPlannedDate(event.startDateLocal()))
                        .append("\n");

                Integer duration = event.timeTargetSeconds() != null
                        ? event.timeTargetSeconds()
                        : event.movingTimeSeconds();

                if (duration != null && duration > 0) {
                    message.append("Duration: ")
                            .append(formatDuration(duration))
                            .append("\n");
                }

                Double distance = event.distanceTargetMeters() != null
                        ? event.distanceTargetMeters()
                        : event.distanceMeters();

                if (distance != null && distance > 0) {
                    message.append("Distance: ")
                            .append(formatDistance(distance))
                            .append("\n");
                }

                if (event.trainingLoad() != null
                        && event.trainingLoad() > 0) {
                    message.append("Training load: ")
                            .append(Math.round(event.trainingLoad()))
                            .append("\n");
                }

                message.append("\n");
            }

            telegramBotClient.sendMessage(
                    chatId,
                    message.toString().trim()
            );

        } catch (Exception exception) {
            log.warn(
                    "Telegram upcoming workouts failed: errorType={}",
                    exception.getClass().getSimpleName()
            );

            telegramBotClient.sendMessage(
                    chatId,
                    "I couldn't load your upcoming workouts right now. "
                            + "Check your Intervals.icu connection and try again."
            );
        }
    }

    private String formatPlannedDate(String value) {
        if (value == null || value.isBlank()) {
            return "Date unavailable";
        }

        try {
            return LocalDateTime.parse(value)
                    .format(DateTimeFormatter.ofPattern(
                            "EEE, dd MMM 'at' HH:mm",
                            Locale.ENGLISH
                    ));
        } catch (RuntimeException exception) {
            try {
                return LocalDate.parse(value.substring(0, 10))
                        .format(DateTimeFormatter.ofPattern(
                                "EEE, dd MMM yyyy",
                                Locale.ENGLISH
                        ));
            } catch (RuntimeException ignored) {
                return value;
            }
        }
    }

    private void sendLastWorkout(long chatId, long telegramUserId) {
        Optional<Long> userId = findLinkedUserId(telegramUserId);

        if (userId.isEmpty()) {
            sendNotLinkedMessage(chatId);
            return;
        }

        Optional<Workout> optionalWorkout =
                workoutRepository.findTopByUserIdOrderByStartedAtDesc(
                        userId.get()
                );

        if (optionalWorkout.isEmpty()) {
            telegramBotClient.sendMessage(
                    chatId,
                    "No workouts found yet. Add a workout or sync Intervals.icu first."
            );
            return;
        }

        Workout workout = optionalWorkout.get();

        StringBuilder response = new StringBuilder()
                .append("🏋️ Your Latest Workout\n\n")
                .append("Type: ")
                .append(formatWorkoutType(workout.getWorkoutType()))
                .append("\n")
                .append("Started: ")
                .append(formatDate(workout.getStartedAt()))
                .append("\n")
                .append("Duration: ")
                .append(formatDuration(workout.getDurationSeconds()))
                .append("\n")
                .append("Distance: ")
                .append(formatDistance(workout.getDistanceMeters()))
                .append("\n")
                .append("Calories: ")
                .append(formatOptionalInteger(workout.getCalories(), " kcal"))
                .append("\n")
                .append("Average heart rate: ")
                .append(formatOptionalInteger(
                        workout.getAverageHeartRate(),
                        " bpm"
                ))
                .append("\n")
                .append("Maximum heart rate: ")
                .append(formatOptionalInteger(
                        workout.getMaxHeartRate(),
                        " bpm"
                ));

        if (workout.getNotes() != null
                && !workout.getNotes().isBlank()) {
            response.append("\nName: ").append(workout.getNotes());
        }

        telegramBotClient.sendMessage(chatId, response.toString());
    }

    private void sendRecentWorkouts(
            long chatId,
            long telegramUserId
    ) {
        Optional<Long> userId = findLinkedUserId(telegramUserId);

        if (userId.isEmpty()) {
            sendNotLinkedMessage(chatId);
            return;
        }

        List<Workout> workouts =
                workoutRepository.findByUserIdOrderByStartedAtDesc(
                        userId.get(),
                        PageRequest.of(0, 5)
                );

        if (workouts.isEmpty()) {
            telegramBotClient.sendMessage(
                    chatId,
                    "No workouts found yet. Add a workout or sync Intervals.icu first."
            );
            return;
        }

        StringBuilder response = new StringBuilder(
                "📋 Your 5 Most Recent Workouts\n\n"
        );

        for (int i = 0; i < workouts.size(); i++) {
            Workout workout = workouts.get(i);

            response.append(i + 1)
                    .append(". ")
                    .append(formatWorkoutType(workout.getWorkoutType()))
                    .append("\n   ")
                    .append(formatDate(workout.getStartedAt()))
                    .append("\n   Duration: ")
                    .append(formatDuration(workout.getDurationSeconds()))
                    .append(" | Distance: ")
                    .append(formatDistance(workout.getDistanceMeters()))
                    .append("\n\n");
        }

        telegramBotClient.sendMessage(chatId, response.toString().trim());
    }

    private void sendSummary(
            long chatId,
            long telegramUserId,
            String argument
    ) {
        Optional<Long> userId = findLinkedUserId(telegramUserId);

        if (userId.isEmpty()) {
            sendNotLinkedMessage(chatId);
            return;
        }

        Integer days = parseSummaryDays(argument);

        if (days == null) {
            telegramBotClient.sendMessage(
                    chatId,
                    """
                    Usage:

                    /summary       - Last 7 days
                    /summary 30d   - Last 30 days
                    /summary 90d   - Last 90 days

                    You can also use /stats with the same arguments.
                    """
            );
            return;
        }

        OffsetDateTime end = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = end.minusDays(days);

        try {
            WorkoutAnalyticsSummary summary =
                    workoutAnalyticsService.getSummary(
                            userId.get(),
                            start,
                            end
                    );

            StringBuilder response = new StringBuilder()
                    .append("📊 Fitness Summary — Last ")
                    .append(days)
                    .append(" Days\n\n")
                    .append("Workouts: ")
                    .append(summary.totalWorkouts())
                    .append("\n")
                    .append("Total duration: ")
                    .append(formatDuration(summary.totalDurationSeconds()))
                    .append("\n")
                    .append("Total distance: ")
                    .append(formatDistance(summary.totalDistanceMeters()))
                    .append("\n")
                    .append("Total calories: ")
                    .append(formatOptionalInteger(
                            summary.totalCalories(), " kcal"
                    ))
                    .append("\n")
                    .append("Average heart rate: ")
                    .append(formatOptionalInteger(
                            summary.averageHeartRate(), " bpm"
                    ))
                    .append("\n")
                    .append("Maximum heart rate: ")
                    .append(formatOptionalInteger(
                            summary.maxHeartRate(), " bpm"
                    ))
                    .append("\n")
                    .append("Average pace: ")
                    .append(formatPace(summary.averagePaceSecondsPerKm()))
                    .append("\n");

            Map<WorkoutType, Long> breakdown =
                    summary.workoutTypeBreakdown();

            if (breakdown != null && !breakdown.isEmpty()) {
                response.append("\nWorkouts by type:\n");

                breakdown.forEach((type, count) ->
                        response.append("• ")
                                .append(formatWorkoutType(type))
                                .append(": ")
                                .append(count)
                                .append("\n")
                );
            }

            telegramBotClient.sendMessage(
                    chatId,
                    response.toString().trim()
            );

        } catch (Exception exception) {
            log.error(
                    "Telegram summary failed; errorType={}",
                    exception.getClass().getSimpleName()
            );

            telegramBotClient.sendMessage(
                    chatId,
                    "I couldn't prepare your summary right now. Please try again later."
            );
        }
    }

    private Integer parseSummaryDays(String argument) {
        if (argument == null || argument.isBlank()) {
            return 7;
        }

        String value = argument.trim().toLowerCase(Locale.ROOT)
                .replace("days", "")
                .replace("day", "")
                .replace("d", "")
                .trim();

        try {
            int days = Integer.parseInt(value);
            return days == 7 || days == 30 || days == 90
                    ? days
                    : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
    private void handleLinkCommand(
            long chatId,
            long telegramUserId,
            String username,
            String code
    ) {
        if (code.isBlank() || code.contains(" ")) {
            telegramBotClient.sendMessage(
                    chatId,
                    "Generate a linking code using the authenticated API, "
                            + "then send /link YOUR_CODE. The code expires in 5 minutes."
            );
            return;
        }

        TelegramLinkService.LinkResult result =
                telegramLinkService.linkAccount(
                        telegramUserId,
                        username,
                        code
                );

        String reply = switch (result) {
            case LINKED ->
                    "✅ Your Telegram account is now linked!";
            case ALREADY_LINKED ->
                    "Your Telegram account is already linked.";
            case TELEGRAM_ACCOUNT_ALREADY_LINKED ->
                    "This Telegram account is linked to another app account.";
            case APP_ACCOUNT_ALREADY_LINKED ->
                    "Your app account is linked to another Telegram account.";
            case INVALID_OR_EXPIRED_CODE ->
                    "The code is invalid or expired. Generate a new one.";
        };

        telegramBotClient.sendMessage(chatId, reply);
    }

    private void sendNotLinkedMessage(long chatId) {
        telegramBotClient.sendMessage(
                chatId,
                "Your account is not linked yet. Use /help for linking instructions."
        );
    }

    private String formatWorkoutType(WorkoutType type) {
        if (type == null) {
            return "Unknown";
        }

        return type.name().replace('_', ' ');
    }

    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }

        return dateTime
                .withOffsetSameInstant(ZoneOffset.UTC)
                .format(DATE_FORMATTER);
    }

    private String formatDuration(Number value) {
        if (value == null || value.longValue() < 0) {
            return "N/A";
        }

        long totalSeconds = value.longValue();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }

        return seconds + "s";
    }

    private String formatDistance(Number meters) {
        if (meters == null || meters.doubleValue() <= 0) {
            return "—";
        }

        return String.format(
                Locale.ROOT,
                "%.2f km",
                meters.doubleValue() / 1000.0
        );
    }

    private String formatOptionalInteger(Number value, String suffix) {
        if (value == null || !Double.isFinite(value.doubleValue())) {
            return "—";
        }

        return Math.round(value.doubleValue()) + suffix;
    }

    private String formatPace(Number secondsPerKm) {
        if (secondsPerKm == null
                || !Double.isFinite(secondsPerKm.doubleValue())
                || secondsPerKm.doubleValue() <= 0) {
            return "—";
        }

        long totalSeconds = Math.round(secondsPerKm.doubleValue());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format(
                Locale.ROOT,
                "%d:%02d min/km",
                minutes,
                seconds
        );
    }
}