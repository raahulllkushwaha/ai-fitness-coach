package com.rahul.aifitness.workout.service;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.workout.dto.request.CreateExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateExerciseRequest;
import com.rahul.aifitness.workout.dto.response.ExerciseResponse;
import com.rahul.aifitness.workout.entity.Exercise;
import com.rahul.aifitness.workout.mapper.ExerciseMapper;
import com.rahul.aifitness.workout.repository.ExerciseRepository;
import com.rahul.aifitness.workout.repository.WorkoutExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final ExerciseMapper exerciseMapper;

    @Transactional
    public ExerciseResponse createExercise(CreateExerciseRequest request) {

        log.info("Creating exercise. name={}", request.name());

        String normalizedName = normalizeName(request.name());
        validateUniqueness(normalizedName);
        Exercise exercise = exerciseMapper.toEntity(request);
        Exercise savedExercise = exerciseRepository.save(exercise);

        log.info(
                "Exercise created successfully. exerciseId={}, name={}",
                savedExercise.getId(),
                savedExercise.getName()
        );

        return exerciseMapper.toResponse(savedExercise);
    }

    public ExerciseResponse getExerciseById(Long exerciseId) {
        log.debug("Fetching exercise. exerciseId={}", exerciseId);

        Exercise exercise = findExerciseById(exerciseId);
        return exerciseMapper.toResponse(exercise);
    }

    public Page<ExerciseResponse> getAllExercises(
            Pageable pageable
    ) {
        log.debug(
                "Fetching exercises. page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return exerciseRepository.findAll(pageable)
                .map(exerciseMapper::toResponse);
    }

    @Transactional
    public ExerciseResponse updateExercise(
            Long exerciseId,
            UpdateExerciseRequest request
    ) {
        log.info(
                "Updating exercise. exerciseId={}",
                exerciseId
        );

        Exercise exercise = findExerciseById(exerciseId);
        String normalizedName = normalizeName(request.name());

        validateUpdateUniqueness(exerciseId, normalizedName);

        exerciseMapper.updateEntity(
                exercise,
                request
        );

        log.info("Exercise updated successfully. exerciseId={}", exerciseId);

        return exerciseMapper.toResponse(exercise);
    }

    @Transactional
    public void deleteExercise(
            Long exerciseId
    ) {
        log.info(
                "Deleting exercise. exerciseId={}",
                exerciseId
        );

        Exercise exercise = findExerciseById(exerciseId);
        validateExerciseDeletion(exerciseId);
        exerciseRepository.delete(exercise);

        log.info(
                "Exercise deleted successfully. exerciseId={}",
                exerciseId
        );
    }

    private Exercise findExerciseById(
            Long exerciseId
    ) {
        return exerciseRepository
                .findById(exerciseId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Exercise not found with id: " + exerciseId
                        )
                );
    }

    private void validateUniqueness(
            String normalizedName
    ) {
        if (exerciseRepository.existsByNormalizedName(normalizedName)) {
            throw new DuplicateResourceException(
                    "Exercise already exists: " + normalizedName
            );
        }
    }

    private void validateUpdateUniqueness(
            Long exerciseId,
            String normalizedName
    ) {
        exerciseRepository
                .findByNormalizedName(normalizedName)
                .ifPresent(existingExercise -> {
                    if (!existingExercise.getId().equals(exerciseId)) {
                        throw new DuplicateResourceException(
                                "Exercise already exists: " + normalizedName
                        );
                    }
                });
    }

    private void validateExerciseDeletion(
            Long exerciseId
    ) {
        if (workoutExerciseRepository.existsByExerciseId(exerciseId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Exercise cannot be deleted because it is already used in a workout",
                    "Exercise in use"
            );
        }
    }

    private String normalizeName(
            String name
    ) {
        return name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}