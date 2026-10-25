package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuSyncResponse;
import com.rahul.aifitness.integration.intervalsicu.entity.IntervalsIcuConnection;
import com.rahul.aifitness.integration.intervalsicu.repository.IntervalsIcuConnectionRepository;
import com.rahul.aifitness.integration.telegram.TelegramBotClient;
import com.rahul.aifitness.integration.telegram.repository.TelegramAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@Slf4j
@RequiredArgsConstructor
public class IntervalsIcuAutoSyncScheduler {

    private final IntervalsIcuConnectionRepository connectionRepository;
    private final IntervalsIcuSyncService syncService;
    private final TelegramAccountLinkRepository telegramAccountLinkRepository;
    private final TelegramBotClient telegramBotClient;

    @Value("${intervals.icu.auto-sync.days-back:3}")
    private int daysBack;

    @Scheduled(
            initialDelayString = "${intervals.icu.auto-sync.initial-delay-ms:120000}",
            fixedDelayString = "${intervals.icu.auto-sync.fixed-delay-ms:1800000}"
    )
    public void syncRecentActivities() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate oldest = today.minusDays(Math.max(daysBack - 1, 0));

        for (IntervalsIcuConnection connection : connectionRepository.findAll()) {
            Long userId = connection.getUserId();

            try {
                IntervalsIcuSyncResponse result =
                        syncService.syncActivities(
                                userId,
                                oldest,
                                today,
                                200
                        );

                if (result.importedWorkouts() > 0) {
                    notifyNewWorkouts(userId, result.importedWorkouts());
                }

                log.info(
                        "Automatic activity sync completed: userId={}, imported={}",
                        userId,
                        result.importedWorkouts()
                );

            } catch (Exception exception) {
                log.warn(
                        "Automatic activity sync failed: userId={}, errorType={}",
                        userId,
                        exception.getClass().getSimpleName()
                );
            }
        }
    }

    private void notifyNewWorkouts(Long userId, int importedCount) {
        if (!telegramBotClient.isConfigured()) {
            return;
        }

        telegramAccountLinkRepository.findByUserId(userId)
                .ifPresent(link -> {
                    try {
                        String workoutWord =
                                importedCount == 1 ? "workout" : "workouts";

                        telegramBotClient.sendMessage(
                                link.getTelegramUserId(),
                                "✅ Activity sync complete!\n\n"
                                        + importedCount
                                        + " new "
                                        + workoutWord
                                        + " added to your AI Fitness Coach account.\n\n"
                                        + "Send /lastworkout to view your latest session."
                        );

                    } catch (Exception exception) {
                        log.warn(
                                "Could not send workout sync notification: userId={}, errorType={}",
                                userId,
                                exception.getClass().getSimpleName()
                        );
                    }
                });
    }
}