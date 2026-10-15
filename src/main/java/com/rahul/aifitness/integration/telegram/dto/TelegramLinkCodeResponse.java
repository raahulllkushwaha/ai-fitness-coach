package com.rahul.aifitness.integration.telegram.dto;

public record TelegramLinkCodeResponse(
        String code,
        long expiresInSeconds,
        String telegramCommand
) {
}