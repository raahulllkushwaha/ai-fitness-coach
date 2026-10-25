package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class IntervalsIcuOAuthStateService {

    private static final String STATE_PREFIX =
            "intervals-icu:oauth:state:";

    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private static final String AUTHORIZE_URL =
            "https://intervals.icu/oauth/authorize";

    private static final String REQUIRED_SCOPE = "ACTIVITY:READ,CALENDAR:READ";

    private final StringRedisTemplate redisTemplate;
    private final String clientId;
    private final String redirectUri;
    private final SecureRandom secureRandom = new SecureRandom();

    public IntervalsIcuOAuthStateService(
            StringRedisTemplate redisTemplate,
            @Value("${INTERVALS_OAUTH_CLIENT_ID:}")
            String clientId,
            @Value("${INTERVALS_OAUTH_REDIRECT_URI:}")
            String redirectUri
    ) {
        this.redisTemplate = redisTemplate;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public String createAuthorizationUrl(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid application user");
        }

        if (!StringUtils.hasText(clientId)
                || !StringUtils.hasText(redirectUri)) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Intervals.icu OAuth is not configured",
                    "The integration is not configured yet"
            );
        }

        String state = generateRandomValue();
        String verifier = generateRandomValue();

        String storedValue = userId + ":" + verifier;

        Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                STATE_PREFIX + state,
                storedValue,
                STATE_TTL
        );

        if (!Boolean.TRUE.equals(stored)) {
            throw new IllegalStateException(
                    "Could not initialize the Intervals.icu authorization request"
            );
        }

        String challenge = createCodeChallenge(verifier);

        return UriComponentsBuilder
                .fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", REQUIRED_SCOPE)
                .queryParam("state", state)
                .queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
    }

    public PendingOAuthState consumeState(String state) {
        if (!StringUtils.hasText(state)) {
            throw invalidState();
        }

        // GETDEL consumes the state atomically; it cannot be reused.
        String storedValue = redisTemplate.opsForValue()
                .getAndDelete(STATE_PREFIX + state);

        if (!StringUtils.hasText(storedValue)) {
            throw invalidState();
        }

        int separator = storedValue.indexOf(':');

        if (separator <= 0 || separator == storedValue.length() - 1) {
            throw invalidState();
        }

        try {
            Long userId = Long.parseLong(
                    storedValue.substring(0, separator)
            );

            String verifier = storedValue.substring(separator + 1);

            if (userId <= 0 || verifier.length() < 43
                    || verifier.length() > 128) {
                throw invalidState();
            }

            return new PendingOAuthState(userId, verifier);

        } catch (NumberFormatException exception) {
            throw invalidState();
        }
    }

    private String generateRandomValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String createCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] challenge = digest.digest(
                    verifier.getBytes(StandardCharsets.US_ASCII)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(challenge);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private BusinessException invalidState() {
        return new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired OAuth request",
                "The authorization request is invalid or expired. Please connect again."
        );
    }

    public record PendingOAuthState(
            Long userId,
            String codeVerifier
    ) {
    }
}