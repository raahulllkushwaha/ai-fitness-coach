package com.rahul.aifitness.integration.telegram;

import com.rahul.aifitness.integration.telegram.dto.TelegramLinkCodeResponse;
import com.rahul.aifitness.integration.telegram.entity.TelegramAccountLink;
import com.rahul.aifitness.integration.telegram.repository.TelegramAccountLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TelegramLinkService {

    private static final String CODE_PREFIX = "telegram:link-code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final long CODE_EXPIRY_SECONDS = CODE_TTL.toSeconds();

    // Avoid ambiguous characters such as I, O, 0 and 1.
    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final StringRedisTemplate redisTemplate;
    private final TelegramAccountLinkRepository linkRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public TelegramLinkCodeResponse generateLinkCode(Long userId) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = generateRandomCode();

            Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                    CODE_PREFIX + code,
                    userId.toString(),
                    CODE_TTL
            );

            if (Boolean.TRUE.equals(stored)) {
                return new TelegramLinkCodeResponse(
                        code,
                        CODE_EXPIRY_SECONDS,
                        "/link " + code
                );
            }
        }

        throw new IllegalStateException(
                "Could not generate a unique Telegram linking code"
        );
    }

    @Transactional
    public LinkResult linkAccount(
            Long telegramUserId,
            String telegramUsername,
            String suppliedCode
    ) {
        if (telegramUserId == null || telegramUserId <= 0
                || !StringUtils.hasText(suppliedCode)) {
            return LinkResult.INVALID_OR_EXPIRED_CODE;
        }

        String code = suppliedCode.trim().toUpperCase(Locale.ROOT);

        if (!code.matches("[A-HJ-NP-Z2-9]{8}")) {
            return LinkResult.INVALID_OR_EXPIRED_CODE;
        }

        // GETDEL consumes the code atomically: it cannot be reused.
        String userIdValue = redisTemplate.opsForValue()
                .getAndDelete(CODE_PREFIX + code);

        if (!StringUtils.hasText(userIdValue)) {
            // Makes retries safe if the account was linked but a reply failed.
            return linkRepository.findByTelegramUserId(telegramUserId)
                    .isPresent()
                    ? LinkResult.ALREADY_LINKED
                    : LinkResult.INVALID_OR_EXPIRED_CODE;
        }

        Long userId;

        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException exception) {
            return LinkResult.INVALID_OR_EXPIRED_CODE;
        }

        var existingTelegram =
                linkRepository.findByTelegramUserId(telegramUserId);

        if (existingTelegram.isPresent()) {
            return existingTelegram.get().getUserId().equals(userId)
                    ? LinkResult.ALREADY_LINKED
                    : LinkResult.TELEGRAM_ACCOUNT_ALREADY_LINKED;
        }

        var existingUser = linkRepository.findByUserId(userId);

        if (existingUser.isPresent()) {
            return LinkResult.APP_ACCOUNT_ALREADY_LINKED;
        }

        linkRepository.saveAndFlush(
                new TelegramAccountLink(
                        userId,
                        telegramUserId,
                        normalizeUsername(telegramUsername)
                )
        );

        return LinkResult.LINKED;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(8);

        for (int i = 0; i < 8; i++) {
            code.append(ALPHABET.charAt(
                    secureRandom.nextInt(ALPHABET.length())
            ));
        }

        return code.toString();
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        String normalized = username.trim();

        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }

        return normalized.length() > 100
                ? normalized.substring(0, 100)
                : normalized;
    }

    public enum LinkResult {
        LINKED,
        ALREADY_LINKED,
        TELEGRAM_ACCOUNT_ALREADY_LINKED,
        APP_ACCOUNT_ALREADY_LINKED,
        INVALID_OR_EXPIRED_CODE
    }
}