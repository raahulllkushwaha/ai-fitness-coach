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
        try {
            var accountLink = accountLinkRepository
                    .findByTelegramUserId(telegramUserId);

            if (accountLink.isEmpty()) {
                telegramBotClient.sendMessage(
                        chatId,
                        "Your account is not linked. Use /help for instructions."
                );
                return;
            }

            if (text == null || text.isBlank() || text.length() > 2000) {
                telegramBotClient.sendMessage(
                        chatId,
                        "Please send a non-empty message under 2000 characters."
                );
                return;
            }

            Long userId = accountLink.get().getUserId();

            long aiStartTime = System.currentTimeMillis();
            log.info("Telegram AI request started: telegramUserId={}", telegramUserId);
            var response = aiConversationService.chat(userId, text);

            log.info(
                    "Telegram AI request completed: telegramUserId={}, durationMs={}",
                    telegramUserId,
                    System.currentTimeMillis() - aiStartTime
            );

            String answer = response.message();

            if (answer == null || answer.isBlank()) {
                answer = "I couldn't generate a response. Please try again later.";
            }

            if (answer.length() > 4000) {
                answer = answer.substring(0, 4000)
                        + "\n\n(Response shortened for Telegram.)";
            }

            telegramBotClient.sendMessage(chatId, answer);

        } catch (Exception exception) {
            log.warn(
                    "Telegram AI handling failed; exceptionType={}, message={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );

            try {
                telegramBotClient.sendMessage(
                        chatId,
                        "Your AI Coach is temporarily unavailable. "
                                + "Please try again later."
                );
            } catch (Exception sendException) {
                log.warn(
                        "Could not send Telegram error response; exceptionType={}",
                        sendException.getClass().getSimpleName()
                );
            }
        }
    }
}