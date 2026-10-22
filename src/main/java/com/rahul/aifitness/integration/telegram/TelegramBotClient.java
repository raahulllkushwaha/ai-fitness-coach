package com.rahul.aifitness.integration.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBotClient {

    private final String botToken;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public TelegramBotClient(@Value("${TELEGRAM_BOT_TOKEN:}") String botToken) {
        this.botToken = botToken;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(botToken);
    }

    public List<JsonNode> getUpdates(long offset) {
        requireToken();

        String response = restClient.get()
                .uri("/bot" + botToken
                        + "/getUpdates?offset=" + offset
                        + "&timeout=10")
                .retrieve()
                .body(String.class);

        JsonNode root = parseResponse(response);

        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "Telegram getUpdates failed: "
                            + root.path("description").asText("Unknown Telegram API error")
            );
        }

        JsonNode results = root.path("result");

        if (!results.isArray()) {
            return List.of();
        }

        List<JsonNode> updates = new ArrayList<>();

        results.forEach(updates::add);

        return updates;
    }

    public void sendMessage(long chatId, String text) {
        requireToken();

        String response = restClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text
                ))
                .retrieve()
                .body(String.class);

        JsonNode root = parseResponse(response);

        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "Telegram sendMessage request was unsuccessful"
            );
        }

    }

    public long sendMessageAndGetId(long chatId, String text) {
        requireToken();

        String response = restClient.post()
                .uri("/bot" + botToken + "/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text
                ))
                .retrieve()
                .body(String.class);

        JsonNode root = parseResponse(response);

        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "Telegram could not send the status message"
            );
        }

        long messageId = root.path("result")
                .path("message_id")
                .asLong(-1);

        if (messageId < 0) {
            throw new IllegalStateException(
                    "Telegram did not return a message ID"
            );
        }

        return messageId;
    }

    public void editMessageText(
            long chatId,
            long messageId,
            String text
    ) {
        requireToken();

        String response = restClient.post()
                .uri("/bot" + botToken + "/editMessageText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", chatId,
                        "message_id", messageId,
                        "text", text
                ))
                .retrieve()
                .body(String.class);

        JsonNode root = parseResponse(response);

        if (!root.path("ok").asBoolean(false)) {
            throw new IllegalStateException(
                    "Telegram could not update the status message"
            );
        }
    }

    private void requireToken() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Configure TELEGRAM_BOT_TOKEN"
            );
        }
    }

    private JsonNode parseResponse(String response) {
        try {
            return objectMapper.readTree(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not parse Telegram API response"
            );
        }
    }
}