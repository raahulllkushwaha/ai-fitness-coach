package com.rahul.aifitness.integration.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.rahul.aifitness.ai.service.AiConversationService;
import com.rahul.aifitness.integration.telegram.repository.TelegramAccountLinkRepository;
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
    private final TelegramLinkService telegramLinkService;
    private final TelegramAccountLinkRepository accountLinkRepository;
    private final AiConversationService aiConversationService;
    private final TelegramAiMessageService telegramAiMessageService;

    private long nextOffset = 0;

    @Scheduled(fixedDelay = 1000)
    public void pollUpdates() {
        if (!telegramBotClient.isConfigured()) {
            log.warn("Telegram polling disabled: bot token is missing");
            return;
        }

        try {
            List<JsonNode> updates =
                    telegramBotClient.getUpdates(nextOffset);

            if (!updates.isEmpty()) {
                log.info(
                        "Telegram received {} update(s), offset={}",
                        updates.size(),
                        nextOffset
                );
            }

            for (JsonNode update : updates) {
                long updateId = update.path("update_id").asLong(-1);

                if (updateId < 0) {
                    continue;
                }

                try {
                    handleUpdate(update);
                    nextOffset = updateId + 1;

                    log.info(
                            "Telegram update handled successfully: updateId={}",
                            updateId
                    );
                } catch (Exception ex) {
                    log.error(
                            "Telegram update processing failed: updateId={}, errorType={}",
                            updateId,
                            ex.getClass().getSimpleName()
                    );
                    break;
                }
            }
        } catch (org.springframework.web.client.RestClientResponseException ex) {
            log.error(
                    "Telegram API HTTP error: status={}, response={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
        } catch (Exception ex) {
            log.error(
                    "Telegram polling failed: errorType={}, message={}",
                    ex.getClass().getSimpleName(),
                    ex.getMessage()
            );
        }
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

        // Accept private chats only.
        if (!"private".equals(chat.path("type").asText())) {
            return;
        }

        long chatId = chat.path("id").asLong();
        long telegramUserId = sender.path("id").asLong();
        String username = sender.path("username").asText(null);
        String text = textNode.asText().trim();

        if (text.isEmpty()) {
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
                    Welcome to AI Fitness Coach! 💪

                    Your Telegram account can be linked to your fitness account.

                    Use /help for available commands.
                    """
            );

            case "/help" -> telegramBotClient.sendMessage(
                    chatId,
                    """
                    AI Fitness Coach — Help

                    /start - Start the bot
                    /help - Show commands
                    /link CODE - Link your fitness account

                    Once linked, send a normal text message
                    to chat with your AI Fitness Coach.
                    """
            );

            case "/link" -> handleLinkCommand(
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
                    telegramAiMessageService.processMessage(
                            chatId,
                            telegramUserId,
                            text
                    );
                }
            }
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
                            + "then send /link YOUR_CODE. "
                            + "The code expires in 5 minutes."
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
                    "✅ Your Telegram account is linked to AI Fitness Coach!";
            case ALREADY_LINKED ->
                    "Your Telegram account is already linked.";
            case TELEGRAM_ACCOUNT_ALREADY_LINKED ->
                    "This Telegram account is linked to another app account.";
            case APP_ACCOUNT_ALREADY_LINKED ->
                    "Your app account is linked to another Telegram account.";
            case INVALID_OR_EXPIRED_CODE ->
                    "The code is invalid or expired. Generate a new code and try again.";
        };

        telegramBotClient.sendMessage(chatId, reply);
    }

    private void handleAiMessage(
            long chatId,
            long telegramUserId,
            String text
    ) {
        var accountLink = accountLinkRepository
                .findByTelegramUserId(telegramUserId);

        if (accountLink.isEmpty()) {
            telegramBotClient.sendMessage(
                    chatId,
                    """
                    Your Telegram account is not linked yet.

                    Generate a linking code from the authenticated
                    AI Fitness Coach API, then send /link YOUR_CODE.
                    """
            );
            return;
        }

        if (text.length() > 2000) {
            telegramBotClient.sendMessage(
                    chatId,
                    "Your message is too long. Please send a message under 2000 characters."
            );
            return;
        }

        try {
            Long userId = accountLink.get().getUserId();

            var response = aiConversationService.chat(userId, text);

            String answer = response.message();

            if (answer == null || answer.isBlank()) {
                answer = "I couldn't generate a response. Please try again.";
            }

            // Telegram messages have a finite text length limit.
            if (answer.length() > 4000) {
                answer = answer.substring(0, 4000)
                        + "\n\n(Response shortened for Telegram.)";
            }

            telegramBotClient.sendMessage(chatId, answer);

        } catch (Exception exception) {
            log.warn(
                    "Telegram AI response failed; exceptionType={}",
                    exception.getClass().getSimpleName()
            );

            telegramBotClient.sendMessage(
                    chatId,
                    "Your AI Coach is temporarily unavailable or its API quota "
                            + "may be exhausted. Please try again later."
            );
        }
    }
}