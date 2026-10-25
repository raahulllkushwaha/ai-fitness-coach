package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsCalendarEventDto;
import com.rahul.aifitness.integration.intervalsicu.entity.IntervalsIcuConnection;
import com.rahul.aifitness.integration.intervalsicu.repository.IntervalsIcuConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IntervalsIcuPlannedWorkoutService {

    private static final int MAX_LIMIT = 100;

    private final IntervalsIcuClient intervalsIcuClient;
    private final IntervalsIcuConnectionRepository connectionRepository;
    private final IntervalsTokenEncryptionService tokenEncryptionService;

    public List<IntervalsCalendarEventDto> getPlannedWorkouts(
            Long userId,
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        validateRequest(oldest, newest, limit);

        IntervalsIcuConnection connection =
                connectionRepository.findByUserId(userId)
                        .orElseThrow(() -> new BusinessException(
                                HttpStatus.CONFLICT,
                                "Intervals.icu account is not connected",
                                "Connect your Intervals.icu account first"
                        ));

        String accessToken = tokenEncryptionService.decrypt(
                connection.getEncryptedAccessToken()
        );

        List<IntervalsCalendarEventDto> events =
                intervalsIcuClient.fetchPlannedWorkouts(accessToken, oldest, newest, limit);

        return events.stream()
                .filter(event -> event != null)
                .filter(event -> event.startDateLocal() != null)
                .filter(event -> event.category() == null
                        || "WORKOUT".equalsIgnoreCase(event.category()))
                .filter(event -> isWithinRange(
                        event.startDateLocal(),
                        oldest,
                        newest
                ))
                .sorted(Comparator.comparing(
                        IntervalsCalendarEventDto::startDateLocal
                ))
                .limit(limit)
                .toList();
    }

    private boolean isWithinRange(
            String startDateLocal,
            LocalDate oldest,
            LocalDate newest
    ) {
        try {
            LocalDate date = LocalDate.parse(startDateLocal.substring(0, 10));
            return !date.isBefore(oldest) && !date.isAfter(newest);

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void validateRequest(
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        if (oldest == null
                || newest == null
                || oldest.isAfter(newest)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid planned-workout date range",
                    "oldest must be on or before newest"
            );
        }

        if (newest.isAfter(oldest.plusDays(90))) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Date range too large",
                    "Request no more than 91 calendar dates at once"
            );
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid limit",
                    "limit must be between 1 and " + MAX_LIMIT
            );
        }
    }
}