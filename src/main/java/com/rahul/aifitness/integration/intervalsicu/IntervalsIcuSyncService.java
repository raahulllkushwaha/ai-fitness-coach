package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsActivityDto;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuSyncResponse;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutRequest;
import com.rahul.aifitness.workout.entity.WorkoutSource;
import com.rahul.aifitness.workout.entity.WorkoutType;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import com.rahul.aifitness.workout.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntervalsIcuSyncService {

    private static final int MAX_LIMIT = 500;
    private static final String EXTERNAL_ID_PREFIX = "intervals-icu:";

    private final IntervalsIcuClient intervalsIcuClient;
    private final WorkoutRepository workoutRepository;
    private final WorkoutService workoutService;

    @Value("${INTERVALS_OWNER_USER_ID:0}")
    private Long ownerUserId;

    public IntervalsIcuSyncResponse syncActivities(
            Long userId,
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        validateOwner(userId);
        validateRequest(oldest, newest, limit);

        List<IntervalsActivityDto> activities =
                intervalsIcuClient.fetchActivities(oldest, newest, limit);

        int imported = 0;
        int duplicates = 0;
        int invalid = 0;

        for (IntervalsActivityDto activity : activities) {
            if (!isValid(activity)) {
                invalid++;
                continue;
            }

            String externalId = EXTERNAL_ID_PREFIX + activity.id();

            if (workoutRepository.existsByUserIdAndExternalId(
                    userId,
                    externalId
            )) {
                duplicates++;
                continue;
            }

            CreateWorkoutRequest request =
                    mapToWorkoutRequest(activity, externalId);

            try {
                workoutService.createWorkout(userId, request);
                imported++;

            } catch (DuplicateResourceException exception) {
                // Protect against a duplicate detected by WorkoutService.
                duplicates++;

            } catch (DataIntegrityViolationException exception) {
                if (isExternalIdConstraintViolation(exception)) {
                    // A concurrent sync may have inserted the same ID.
                    duplicates++;
                } else {
                    throw exception;
                }
            }
        }

        log.info(
                "Intervals.icu sync completed. userId={}, fetched={}, imported={}, duplicates={}, invalid={}",
                userId,
                activities.size(),
                imported,
                duplicates,
                invalid
        );

        return new IntervalsIcuSyncResponse(
                oldest,
                newest,
                activities.size(),
                imported,
                duplicates,
                invalid
        );
    }

    private void validateOwner(Long userId) {
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure INTERVALS_OWNER_USER_ID for the account associated with this API key",
                    "Intervals.icu integration is not configured"
            );
        }

        if (!ownerUserId.equals(userId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "The configured Intervals.icu personal API key is restricted to its owner account",
                    "Integration access denied"
            );
        }
    }

    private void validateRequest(
            LocalDate oldest,
            LocalDate newest,
            int limit
    ) {
        if (oldest == null || newest == null || oldest.isAfter(newest)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "oldest must be on or before newest",
                    "Invalid activity date range"
            );
        }

        if (ChronoUnit.DAYS.between(oldest, newest) > 366) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Sync at most 367 calendar dates per request",
                    "Activity date range too large"
            );
        }

        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + MAX_LIMIT,
                    "Invalid activity limit"
            );
        }
    }

    private boolean isValid(IntervalsActivityDto activity) {
        return activity != null
                && activity.id() != null
                && !activity.id().isBlank()
                && resolveStartTime(activity) != null
                && resolveDuration(activity) != null;
    }

    private CreateWorkoutRequest mapToWorkoutRequest(
            IntervalsActivityDto activity,
            String externalId
    ) {
        OffsetDateTime startedAt = resolveStartTime(activity);

        Integer durationSeconds = resolveDuration(activity);

        int elapsedSeconds =
                activity.elapsedTimeSeconds() != null
                        && activity.elapsedTimeSeconds() > 0
                        ? activity.elapsedTimeSeconds()
                        : durationSeconds;

        // Avoid endedAt being earlier than the duration stored for the workout.
        elapsedSeconds = Math.max(elapsedSeconds, durationSeconds);

        OffsetDateTime endedAt = startedAt.plusSeconds(elapsedSeconds);

        Double distanceMeters = nonNegative(activity.distanceMeters());
        Double elevationGainMeters =
                nonNegative(activity.elevationGainMeters());
        Double calories = nonNegative(activity.calories());

        Double paceSecondsPerKm = null;

        if (distanceMeters != null && distanceMeters > 0) {
            paceSecondsPerKm =
                    durationSeconds * 1000.0 / distanceMeters;
        }

        return new CreateWorkoutRequest(
                mapWorkoutType(activity.type()),
                startedAt,
                endedAt,
                durationSeconds,
                distanceMeters,
                elevationGainMeters,
                toPositiveInteger(activity.averageHeartRate()),
                toPositiveInteger(activity.maxHeartRate()),
                calories,
                paceSecondsPerKm,
                WorkoutSource.INTERVALS_ICU,
                externalId,
                activity.name()
        );
    }

    private OffsetDateTime resolveStartTime(
            IntervalsActivityDto activity
    ) {
        if (activity.startDate() != null) {
            return activity.startDate().withOffsetSameInstant(ZoneOffset.UTC);
        }

        LocalDateTime local = activity.startDateLocal();

        // Fallback only: start_date normally supplies the absolute timestamp.
        return local == null
                ? null
                : local.atOffset(ZoneOffset.UTC);
    }

    private Integer resolveDuration(IntervalsActivityDto activity) {
        Integer moving = activity.movingTimeSeconds();

        if (moving != null && moving > 0) {
            return moving;
        }

        Integer elapsed = activity.elapsedTimeSeconds();

        return elapsed != null && elapsed > 0 ? elapsed : null;
    }

    private Double nonNegative(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0) {
            return null;
        }

        return value;
    }

    private Integer toPositiveInteger(Double value) {
        if (value == null
                || !Double.isFinite(value)
                || value <= 0
                || value > Integer.MAX_VALUE) {
            return null;
        }

        return (int) Math.round(value);
    }

    private WorkoutType mapWorkoutType(String externalType) {
        if (externalType == null || externalType.isBlank()) {
            return WorkoutType.OTHER;
        }

        String type = externalType
                .replaceAll("[^a-zA-Z]", "")
                .toLowerCase(Locale.ROOT);

        return switch (type) {
            case "run", "running", "virtualrun", "trailrun",
                 "treadmill", "indoorrun" -> WorkoutType.RUNNING;

            case "ride", "cycling", "virtualride", "mountainbikeride",
                 "gravelride", "ebikeride", "emountainbikeride",
                 "indoorcycling" -> WorkoutType.CYCLING;

            case "walk", "walking" -> WorkoutType.WALKING;

            case "hike", "hiking" -> WorkoutType.HIKING;

            case "swim", "swimming" -> WorkoutType.SWIMMING;

            case "weighttraining", "strengthtraining", "strength",
                 "workout" -> WorkoutType.STRENGTH;

            default -> WorkoutType.OTHER;
        };
    }

    private boolean isExternalIdConstraintViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && "uq_workouts_user_external_id".equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}