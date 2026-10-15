package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsActivityDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class IntervalsIcuClient {

    private final RestClient restClient;
    private final String apiKey;

    public IntervalsIcuClient(
            @Value("${intervals.icu.base-url:https://intervals.icu/api/v1}")
            String baseUrl,
            @Value("${INTERVALS_API_KEY:}") String apiKey
    ) {
        this.apiKey = apiKey;

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers ->
                        headers.setBasicAuth("API_KEY", apiKey))
                .build();
    }

    public List<IntervalsActivityDto> fetchActivities(
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure INTERVALS_API_KEY before syncing activities",
                    "Intervals.icu integration is not configured"
            );
        }

        try {
            List<IntervalsActivityDto> activities = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/athlete/0/activities")
                            .queryParam("oldest", oldest)
                            .queryParam("newest", newest)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return activities == null ? List.of() : activities;

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            log.warn(
                    "Intervals.icu API returned an unsuccessful status: {}",
                    status
            );

            if (status == 429) {
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Intervals.icu rate limit reached. Please retry later.",
                        "External API rate limit"
                );
            }

            if (status == 401 || status == 403) {
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "Intervals.icu rejected the configured credentials",
                        "External API authentication failed"
                );
            }

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu could not complete the activities request",
                    "External API request failed"
            );

        } catch (RestClientException exception) {
            log.warn(
                    "Could not communicate with Intervals.icu",
                    exception
            );

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not communicate with Intervals.icu",
                    "External API unavailable"
            );
        }
    }
}