package com.rahul.aifitness.workout.service;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.user.entity.User;
import com.rahul.aifitness.user.repository.UserRepository;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutResponse;
import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.mapper.WorkoutMapper;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rahul.aifitness.event.kafka.KafkaEventPublisher;
import com.rahul.aifitness.workout.event.WorkoutEvent;
import com.rahul.aifitness.workout.event.WorkoutEventType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final UserRepository userRepository;
    private final WorkoutMapper workoutMapper;

    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public WorkoutResponse createWorkout(Long userId, CreateWorkoutRequest request) {
        log.info(
                "Creating workout. userId={}, workoutType={}, source={}",
                userId,
                request.workoutType(),
                request.source()
        );

        User user = findUserById(userId);

        validateWorkoutTime(request.startedAt(), request.endedAt());
        validateHeartRate(
                request.averageHeartRate(),
                request.maxHeartRate()
        );

        String externalId = normalizeExternalId(request.externalId());

        validateExternalIdUniqueness(userId, externalId);

        CreateWorkoutRequest normalizedRequest = new CreateWorkoutRequest(
                request.workoutType(),
                request.startedAt(),
                request.endedAt(),
                request.durationSeconds(),
                request.distanceMeters(),
                request.elevationGainMeters(),
                request.averageHeartRate(),
                request.maxHeartRate(),
                request.calories(),
                request.averagePaceSecondsPerKm(),
                request.source(),
                externalId,
                normalizeNotes(request.notes())
        );

        Workout workout = workoutMapper.toEntity(
                normalizedRequest,
                user
        );

        Workout savedWorkout = workoutRepository.save(workout);
        WorkoutEvent event = new WorkoutEvent(
                java.util.UUID.randomUUID(),
                WorkoutEventType.WORKOUT_CREATED,
                savedWorkout.getId(),
                userId,
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
        );

        kafkaEventPublisher.publishWorkoutEvent(event);

        log.info(
                "Workout created successfully. workoutId={}, userId={}",
                savedWorkout.getId(),
                userId
        );

        return workoutMapper.toResponse(savedWorkout);
    }

    public WorkoutResponse getWorkoutById(Long userId, Long workoutId) {
        log.debug(
                "Fetching workout. workoutId={}, userId={}",
                workoutId,
                userId
        );

        Workout workout = findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        return workoutMapper.toResponse(workout);
    }

    public Page<WorkoutResponse> getAllWorkouts(Long userId, Pageable pageable
    ) {
        log.debug(
                "Fetching workouts. userId={}, page={}, size={}",
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        ensureUserExists(userId);

        return workoutRepository
                .findByUserId(userId, pageable)
                .map(workoutMapper::toResponse);
    }

    public Page<WorkoutResponse> getWorkoutsByType(Long userId, com.rahul.aifitness.workout.entity.WorkoutType workoutType, Pageable pageable
    ) {
        log.debug(
                "Fetching workouts by type. userId={}, workoutType={}, page={}, size={}",
                userId,
                workoutType,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        ensureUserExists(userId);

        return workoutRepository
                .findByUserIdAndWorkoutType(
                        userId,
                        workoutType,
                        pageable
                )
                .map(workoutMapper::toResponse);
    }

    public Page<WorkoutResponse> getWorkoutsBetween(
            Long userId,
            java.time.OffsetDateTime start,
            java.time.OffsetDateTime end,
            Pageable pageable
    ) {
        log.debug(
                "Fetching workouts by date range. userId={}, start={}, end={}",
                userId,
                start,
                end
        );

        if (start == null || end == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Start and end timestamps are required",
                    "Invalid date range"

            );
        }

        if (end.isBefore(start)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "End timestamp must be after or equal to start timestamp",
                    "Invalid date range"
            );
        }

        ensureUserExists(userId);

        return workoutRepository
                .findByUserIdAndStartedAtBetween(
                        userId,
                        start,
                        end,
                        pageable
                )
                .map(workoutMapper::toResponse);
    }

    @Transactional
    public WorkoutResponse updateWorkout(Long userId, Long workoutId, UpdateWorkoutRequest request) {
        log.info(
                "Updating workout. workoutId={}, userId={}",
                workoutId,
                userId
        );

        Workout workout = findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        validateWorkoutTime(
                request.startedAt(),
                request.endedAt()
        );

        validateHeartRate(
                request.averageHeartRate(),
                request.maxHeartRate()
        );

        String externalId = normalizeExternalId(
                request.externalId()
        );

        validateExternalIdForUpdate(
                userId,
                workout,
                externalId
        );

        UpdateWorkoutRequest normalizedRequest = new UpdateWorkoutRequest(
                request.workoutType(),
                request.startedAt(),
                request.endedAt(),
                request.durationSeconds(),
                request.distanceMeters(),
                request.elevationGainMeters(),
                request.averageHeartRate(),
                request.maxHeartRate(),
                request.calories(),
                request.averagePaceSecondsPerKm(),
                request.source(),
                externalId,
                normalizeNotes(request.notes())
        );

        workoutMapper.updateEntity(
                workout,
                normalizedRequest
        );
        WorkoutEvent event = new WorkoutEvent(
                UUID.randomUUID(),
                WorkoutEventType.WORKOUT_UPDATED,
                workout.getId(),
                userId,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        kafkaEventPublisher.publishWorkoutEvent(event);

        log.info(
                "Workout updated successfully. workoutId={}, userId={}",
                workoutId,
                userId
        );

        return workoutMapper.toResponse(workout);
    }

    @Transactional
    public void deleteWorkout(
            Long userId,
            Long workoutId
    ) {
        log.info(
                "Deleting workout. workoutId={}, userId={}",
                workoutId,
                userId
        );

        Workout workout = findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        workoutRepository.delete(workout);
        WorkoutEvent event = new WorkoutEvent(
                UUID.randomUUID(),
                WorkoutEventType.WORKOUT_DELETED,
                workoutId,
                userId,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        kafkaEventPublisher.publishWorkoutEvent(event);


        log.info(
                "Workout deleted successfully. workoutId={}, userId={}",
                workoutId,
                userId
        );
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User not found with id: " + userId
                        )
                );
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }
    }

    private Workout findWorkoutByIdAndUserId(
            Long workoutId,
            Long userId
    ) {
        return workoutRepository
                .findByIdAndUserId(workoutId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Workout not found with id: " + workoutId
                        )
                );
    }

    private void validateExternalIdUniqueness(
            Long userId,
            String externalId
    ) {
        if (externalId == null) {
            return;
        }

        if (workoutRepository.existsByUserIdAndExternalId(
                userId,
                externalId
        )) {
            throw new DuplicateResourceException(
                    "Workout with external id already exists: " + externalId
            );
        }
    }

    private void validateExternalIdForUpdate(
            Long userId,
            Workout workout,
            String externalId
    ) {
        if (externalId == null) {
            return;
        }

        String currentExternalId = workout.getExternalId();

        if (externalId.equals(currentExternalId)) {
            return;
        }

        if (workoutRepository.existsByUserIdAndExternalId(
                userId,
                externalId
        )) {
            throw new DuplicateResourceException(
                    "Workout with external id already exists: " + externalId
            );
        }
    }

    private void validateWorkoutTime(
            java.time.OffsetDateTime startedAt,
            java.time.OffsetDateTime endedAt
    ) {
        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Workout end time cannot be before start time",
                    "Invalid workout time"
            );
        }
    }

    private void validateHeartRate(
            Integer averageHeartRate,
            Integer maxHeartRate
    ) {
        if (
                averageHeartRate != null
                        && maxHeartRate != null
                        && averageHeartRate > maxHeartRate
        ) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Average heart rate cannot be greater than maximum heart rate",
                    "Invalid heart rate"
            );
        }
    }

    private String normalizeExternalId(String externalId) {
        if (externalId == null) {
            return null;
        }

        String normalized = externalId.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }

        String normalized = notes.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}