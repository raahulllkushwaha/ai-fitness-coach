package com.rahul.aifitness.integration.telegram;

import com.rahul.aifitness.ai.service.AiConversationService;
import com.rahul.aifitness.integration.telegram.repository.TelegramAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramAiMessageService {

    private final TelegramBotClient telegramBotClient;
    private final TelegramAccountLinkRepository accountLinkRepository;
    private final AiConversationService aiConversationService;

    @Async("telegramAiExecutor")
    public void processMessage(
            long chatId,
            long telegramUserId,
            String text
    ) {
        if (text == null || text.isBlank() || text.length() > 2000) {
            telegramBotClient.sendMessage(
                    chatId,
                    "Please send a non-empty message under 2000 characters."
            );
            return;
        }

        var accountLink = accountLinkRepository
                .findByTelegramUserId(telegramUserId);

        if (accountLink.isEmpty()) {
            telegramBotClient.sendMessage(
                    chatId,
                    "Your account is not linked. Use /help for instructions."
            );
            return;
        }

        final long statusMessageId;

        try {
            statusMessageId = telegramBotClient.sendMessageAndGetId(
                    chatId,
                    "🧠 Thinking... I'll get back to you shortly."
            );
        } catch (Exception exception) {
            log.warn(
                    "Could not send Telegram thinking message; errorType={}",
                    exception.getClass().getSimpleName()
            );
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            var response = aiConversationService.chat(
                    accountLink.get().getUserId(),
                    text
            );

            long durationMs = System.currentTimeMillis() - startTime;

            log.info(
                    "Telegram AI request completed; telegramUserId={}, durationMs={}",
                    telegramUserId,
                    durationMs
            );

            String answer = response.message();

            if (answer == null || answer.isBlank()) {
                answer = "I couldn't generate a response. Please try again later.";
            }

            if (answer.length() > 4000) {
                answer = answer.substring(0, 4000)
                        + "\n\n(Response shortened for Telegram.)";
            }

            telegramBotClient.editMessageText(
                    chatId,
                    statusMessageId,
                    answer
            );

        } catch (Exception exception) {
            String errorMessage = exception.getMessage();

            boolean quotaError = errorMessage != null
                    && (errorMessage.toLowerCase().contains("quota")
                    || errorMessage.toLowerCase().contains("rate limit"));

            log.warn(
                    "Telegram AI request failed; errorType={}, quotaError={}",
                    exception.getClass().getSimpleName(),
                    quotaError
            );

            String userMessage = quotaError
                    ? """
                      ⚠️ The AI service quota or rate limit is currently exhausted.

                      I haven't retried the request. Please try again after the quota resets.

                      Your commands such as /lastworkout, /recent and /summary remain available.
                      """
                    : """
                      ⚠️ I couldn't generate an AI response right now.

                      Please try again later. Your fitness commands remain available.
                      """;

            try {
                telegramBotClient.editMessageText(
                        chatId,
                        statusMessageId,
                        userMessage
                );
            } catch (Exception editException) {
                log.warn(
                        "Could not update Telegram status message; errorType={}",
                        editException.getClass().getSimpleName()
                );
            }
        }
    }
}