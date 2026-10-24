package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuTokenResponse;
import com.rahul.aifitness.integration.intervalsicu.entity.IntervalsIcuConnection;
import com.rahul.aifitness.integration.intervalsicu.repository.IntervalsIcuConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;

@Service
@Slf4j
public class IntervalsIcuOAuthService {

    private static final String TOKEN_URL =
            "https://intervals.icu/api/oauth/token";

    private final IntervalsIcuOAuthStateService stateService;
    private final IntervalsTokenEncryptionService encryptionService;
    private final IntervalsIcuConnectionRepository connectionRepository;
    private final RestClient restClient;

    private final String clientId;
    private final String clientSecret;

    public IntervalsIcuOAuthService(
            IntervalsIcuOAuthStateService stateService,
            IntervalsTokenEncryptionService encryptionService,
            IntervalsIcuConnectionRepository connectionRepository,
            @Value("${INTERVALS_OAUTH_CLIENT_ID:}") String clientId,
            @Value("${INTERVALS_OAUTH_CLIENT_SECRET:}") String clientSecret
    ) {
        this.stateService = stateService;
        this.encryptionService = encryptionService;
        this.connectionRepository = connectionRepository;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.create();
    }

    public void completeAuthorization(String code, String state) {
        if (!StringUtils.hasText(clientId)
                || !StringUtils.hasText(clientSecret)) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Intervals.icu OAuth is not configured",
                    "The integration is not configured yet"
            );
        }

        if (!StringUtils.hasText(code)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Authorization code missing",
                    "Intervals.icu did not return an authorization code"
            );
        }

        var pending = stateService.consumeState(state);

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("code_verifier", pending.codeVerifier());

        final IntervalsIcuTokenResponse tokenResponse;

        try {
            tokenResponse = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(IntervalsIcuTokenResponse.class);

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Intervals.icu token exchange failed with HTTP status {}",
                    exception.getStatusCode().value()
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu authorization failed",
                    "Could not exchange the authorization code. Please connect again."
            );

        } catch (RestClientException exception) {
            log.warn("Could not reach the Intervals.icu token endpoint");

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu is unavailable",
                    "Could not complete authorization. Please try again."
            );
        }

        if (tokenResponse == null
                || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid token response",
                    "Intervals.icu did not return a usable access token"
            );
        }

        String scopes = tokenResponse.scope();

        boolean activityReadGranted =
                StringUtils.hasText(scopes)
                        && Arrays.stream(scopes.split("[,\\s]+"))
                        .anyMatch("ACTIVITY:READ"::equals);

        if (!activityReadGranted) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Activity permission not granted",
                    "Please reconnect and grant ACTIVITY:READ permission"
            );
        }

        String athleteId = tokenResponse.athlete() == null
                ? null
                : tokenResponse.athlete().id();

        String encryptedToken =
                encryptionService.encrypt(tokenResponse.accessToken());

        Long userId = pending.userId();

        IntervalsIcuConnection connection =
                connectionRepository.findByUserId(userId)
                        .map(existing -> {
                            existing.updateCredentials(
                                    encryptedToken,
                                    scopes,
                                    athleteId
                            );
                            return existing;
                        })
                        .orElseGet(() -> new IntervalsIcuConnection(
                                userId,
                                encryptedToken,
                                scopes,
                                athleteId
                        ));

        connectionRepository.save(connection);

        log.info(
                "Intervals.icu account connected for application userId={}",
                userId
        );
    }
}