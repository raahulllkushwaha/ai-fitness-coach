package com.rahul.aifitness.integration.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramUpdatePoller {

    private final TelegramBotClient telegramBotClient;

    private long nextOffset = 0;

    @Scheduled(fixedDelay = 1000)
    public void pollUpdates() {
        if (!telegramBotClient.isConfigured()) {
            return;
        }

        try {
            List<JsonNode> updates =
                    telegramBotClient.getUpdates(nextOffset);

            for (JsonNode update : updates) {
                long updateId = update.path("update_id").asLong(-1);

                if (updateId < 0) {
                    continue;
                }

                handleUpdate(update);

                // Advance offset only after handling the update.
                nextOffset = updateId + 1;
            }

        } catch (Exception exception) {
            // Do not log the exception or URL because the URL contains
            // the Telegram bot token.
            log.warn(
                    "Telegram polling failed; exceptionType={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");

        if (!message.isObject()) {
            return;
        }

        JsonNode chat = message.path("chat");
        JsonNode textNode = message.path("text");

        if (!chat.hasNonNull("id") || !textNode.isTextual()) {
            return;
        }

        long chatId = chat.path("id").asLong();
        String text = textNode.asText().trim();

        if (text.isEmpty()) {
            return;
        }

        String command = text.split("\\s+", 2)[0]
                .split("@", 2)[0]
                .toLowerCase(Locale.ROOT);

        switch (command) {
            case "/start" -> telegramBotClient.sendMessage(
                    chatId,
                    """
                    Welcome to AI Fitness Coach! 💪

                    Your Telegram bot is connected.

                    Use /help to see available commands.
                    Account linking and AI chat will be enabled next.
                    """
            );

            case "/help" -> telegramBotClient.sendMessage(
                    chatId,
                    """
                    AI Fitness Coach — Help

                    /start - Start the bot
                    /help - Show available commands

                    Workout data and AI chat will become available
                    after secure account linking is implemented.
                    """
            );

            default -> telegramBotClient.sendMessage(
                    chatId,
                    "The bot is online! Please use /start or /help. "
                            + "AI chat is coming in the next step."
            );
        }
    }
}