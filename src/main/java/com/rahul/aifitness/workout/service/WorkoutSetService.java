package com.rahul.aifitness.workout.service;

import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutSetRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutSetResponse;
import com.rahul.aifitness.workout.entity.WorkoutExercise;
import com.rahul.aifitness.workout.entity.WorkoutSet;
import com.rahul.aifitness.workout.mapper.WorkoutSetMapper;
import com.rahul.aifitness.workout.repository.WorkoutExerciseRepository;
import com.rahul.aifitness.workout.repository.WorkoutSetRepository;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutSetService {

    private final WorkoutSetRepository workoutSetRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutRepository workoutRepository;
    private final WorkoutSetMapper workoutSetMapper;

    @Transactional
    public WorkoutSetResponse addSetToWorkoutExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            CreateWorkoutSetRequest request
    ) {
        log.info(
                "Adding set. userId={}, workoutId={}, workoutExerciseId={}, setNumber={}",
                userId,
                workoutId,
                workoutExerciseId,
                request.setNumber()
        );

        WorkoutExercise workoutExercise =
                findWorkoutExercise(
                        userId,
                        workoutId,
                        workoutExerciseId
                );

        validateDuplicateSetNumber(
                workoutExerciseId,
                request.setNumber()
        );

        WorkoutSet workoutSet =
                workoutSetMapper.toEntity(
                        request,
                        workoutExercise
                );

        WorkoutSet savedWorkoutSet =
                workoutSetRepository.save(workoutSet);

        log.info(
                "Workout set added successfully. workoutSetId={}, workoutExerciseId={}",
                savedWorkoutSet.getId(),
                workoutExerciseId
        );

        return workoutSetMapper.toResponse(
                savedWorkoutSet
        );
    }

    public List<WorkoutSetResponse> getWorkoutSets(
            Long userId,
            Long workoutId,
            Long workoutExerciseId
    ) {
        log.debug(
                "Fetching workout sets. userId={}, workoutId={}, workoutExerciseId={}",
                userId,
                workoutId,
                workoutExerciseId
        );

        findWorkoutExercise(
                userId,
                workoutId,
                workoutExerciseId
        );

        return workoutSetRepository
                .findByWorkoutExerciseIdOrderBySetNumberAsc(
                        workoutExerciseId
                )
                .stream()
                .map(workoutSetMapper::toResponse)
                .toList();
    }

    public WorkoutSetResponse getWorkoutSetById(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long workoutSetId
    ) {
        log.debug(
                "Fetching workout set. userId={}, workoutId={}, workoutExerciseId={}, workoutSetId={}",
                userId,
                workoutId,
                workoutExerciseId,
                workoutSetId
        );

        findWorkoutExercise(
                userId,
                workoutId,
                workoutExerciseId
        );

        WorkoutSet workoutSet =
                findWorkoutSetById(workoutSetId);

        validateWorkoutSetOwnership(
                workoutSet,
                workoutExerciseId
        );

        return workoutSetMapper.toResponse(
                workoutSet
        );
    }

    @Transactional
    public WorkoutSetResponse updateWorkoutSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long workoutSetId,
            UpdateWorkoutSetRequest request
    ) {
        log.info(
                "Updating workout set. userId={}, workoutId={}, workoutExerciseId={}, workoutSetId={}",
                userId,
                workoutId,
                workoutExerciseId,
                workoutSetId
        );

        findWorkoutExercise(
                userId,
                workoutId,
                workoutExerciseId
        );

        WorkoutSet workoutSet =
                findWorkoutSetById(workoutSetId);

        validateWorkoutSetOwnership(
                workoutSet,
                workoutExerciseId
        );

        validateDuplicateSetNumberForUpdate(
                workoutExerciseId,
                workoutSet,
                request.setNumber()
        );

        workoutSetMapper.updateEntity(
                workoutSet,
                request
        );

        log.info(
                "Workout set updated successfully. workoutSetId={}",
                workoutSetId
        );

        return workoutSetMapper.toResponse(
                workoutSet
        );
    }

    @Transactional
    public void deleteWorkoutSet(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            Long workoutSetId
    ) {
        log.info(
                "Deleting workout set. userId={}, workoutId={}, workoutExerciseId={}, workoutSetId={}",
                userId,
                workoutId,
                workoutExerciseId,
                workoutSetId
        );

        findWorkoutExercise(
                userId,
                workoutId,
                workoutExerciseId
        );

        WorkoutSet workoutSet =
                findWorkoutSetById(workoutSetId);

        validateWorkoutSetOwnership(
                workoutSet,
                workoutExerciseId
        );

        workoutSetRepository.delete(workoutSet);

        log.info(
                "Workout set deleted successfully. workoutSetId={}",
                workoutSetId
        );
    }

    private WorkoutExercise findWorkoutExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId
    ) {
        workoutRepository
                .findByIdAndUserId(workoutId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Workout not found with id: " + workoutId
                        )
                );

        return workoutExerciseRepository
                .findById(workoutExerciseId)
                .filter(
                        workoutExercise ->
                                workoutExercise
                                        .getWorkout()
                                        .getId()
                                        .equals(workoutId)
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Workout exercise not found with id: "
                                        + workoutExerciseId
                        )
                );
    }

    private WorkoutSet findWorkoutSetById(
            Long workoutSetId
    ) {
        return workoutSetRepository
                .findById(workoutSetId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Workout set not found with id: "
                                        + workoutSetId
                        )
                );
    }

    private void validateDuplicateSetNumber(
            Long workoutExerciseId,
            Integer setNumber
    ) {
        boolean alreadyExists =
                workoutSetRepository
                        .findByWorkoutExerciseIdOrderBySetNumberAsc(
                                workoutExerciseId
                        )
                        .stream()
                        .anyMatch(
                                workoutSet ->
                                        workoutSet
                                                .getSetNumber()
                                                .equals(setNumber)
                        );

        if (alreadyExists) {
            throw new DuplicateResourceException(
                    "Set number is already used in this exercise: "
                            + setNumber
            );
        }
    }

    private void validateDuplicateSetNumberForUpdate(
            Long workoutExerciseId,
            WorkoutSet currentWorkoutSet,
            Integer newSetNumber
    ) {
        if (
                currentWorkoutSet
                        .getSetNumber()
                        .equals(newSetNumber)
        ) {
            return;
        }

        boolean alreadyExists =
                workoutSetRepository
                        .findByWorkoutExerciseIdOrderBySetNumberAsc(
                                workoutExerciseId
                        )
                        .stream()
                        .anyMatch(
                                workoutSet ->
                                        !workoutSet
                                                .getId()
                                                .equals(
                                                        currentWorkoutSet.getId()
                                                )
                                                && workoutSet
                                                .getSetNumber()
                                                .equals(newSetNumber)
                        );

        if (alreadyExists) {
            throw new DuplicateResourceException(
                    "Set number is already used in this exercise: "
                            + newSetNumber
            );
        }
    }

    private void validateWorkoutSetOwnership(
            WorkoutSet workoutSet,
            Long workoutExerciseId
    ) {
        if (
                !workoutSet
                        .getWorkoutExercise()
                        .getId()
                        .equals(workoutExerciseId)
        ) {
            throw new ResourceNotFoundException(
                    "Workout set not found with id: "
                            + workoutSet.getId()
            );
        }
    }
}