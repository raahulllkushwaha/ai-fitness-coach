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
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsCalendarEventDto;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class IntervalsIcuClient {

    private final RestClient restClient;

    public IntervalsIcuClient(
            @Value("${intervals.icu.base-url:https://intervals.icu/api/v1}")
            String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<IntervalsActivityDto> fetchActivities(
            String accessToken,
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Intervals.icu account is not connected",
                    "Connect your Intervals.icu account before syncing activities"
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
                    .headers(headers -> headers.setBearerAuth(accessToken))
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
                        "Intervals.icu rate limit reached",
                        "Please retry activity synchronization later"
                );
            }

            if (status == 401 || status == 403) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "Intervals.icu authorization is no longer valid",
                        "Reconnect your Intervals.icu account and try again"
                );
            }

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu request failed",
                    "Intervals.icu could not complete the activities request"
            );

        } catch (RestClientException exception) {
            log.warn("Could not communicate with Intervals.icu");

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu is unavailable",
                    "Could not communicate with Intervals.icu"
            );
        }
    }
    public List<IntervalsCalendarEventDto> fetchPlannedWorkouts(
            String accessToken,
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Intervals.icu account is not connected",
                    "Connect your Intervals.icu account before loading planned workouts"
            );
        }

        try {
            List<IntervalsCalendarEventDto> events = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/athlete/0/events")
                            .queryParam("oldest", oldest)
                            .queryParam("newest", newest)
                            .queryParam("category", "WORKOUT")
                            .queryParam("limit", limit)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IntervalsCalendarEventDto>>() {});

            return events == null ? List.of() : events;

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            log.warn(
                    "Intervals.icu calendar API returned HTTP status {}",
                    status
            );

            if (status == 401 || status == 403) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "Intervals.icu calendar permission is missing or invalid",
                        "Reconnect Intervals.icu and grant CALENDAR:READ permission"
                );
            }

            if (status == 429) {
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Intervals.icu rate limit reached",
                        "Please try loading planned workouts later"
                );
            }

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu calendar request failed",
                    "Could not load planned workouts from Intervals.icu"
            );

        } catch (RestClientException exception) {
            log.warn("Could not communicate with the Intervals.icu calendar API");

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "Intervals.icu is unavailable",
                    "Could not load planned workouts"
            );
        }
    }

    public boolean revokeAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        try {
            restClient.delete()
                    .uri("/disconnect-app")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Intervals.icu token revocation returned HTTP status {}",
                    exception.getStatusCode().value()
            );
            return false;

        } catch (RestClientException exception) {
            log.warn("Could not reach Intervals.icu for token revocation");
            return false;
        }
    }
}