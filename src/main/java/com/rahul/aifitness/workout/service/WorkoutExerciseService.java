package com.rahul.aifitness.workout.service;

import com.rahul.aifitness.common.exception.BusinessException;
import com.rahul.aifitness.common.exception.DuplicateResourceException;
import com.rahul.aifitness.common.exception.ResourceNotFoundException;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutExerciseRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutExerciseResponse;
import com.rahul.aifitness.workout.entity.Exercise;
import com.rahul.aifitness.workout.entity.Workout;
import com.rahul.aifitness.workout.entity.WorkoutExercise;
import com.rahul.aifitness.workout.mapper.WorkoutExerciseMapper;
import com.rahul.aifitness.workout.repository.ExerciseRepository;
import com.rahul.aifitness.workout.repository.WorkoutExerciseRepository;
import com.rahul.aifitness.workout.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutExerciseService {

    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutExerciseMapper workoutExerciseMapper;

    @Transactional
    public WorkoutExerciseResponse addExerciseToWorkout(
            Long userId,
            Long workoutId,
            CreateWorkoutExerciseRequest request
    ) {
        log.info(
                "Adding exercise to workout. userId={}, workoutId={}, exerciseId={}",
                userId,
                workoutId,
                request.exerciseId()
        );

        Workout workout = findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        Exercise exercise = findExerciseById(
                request.exerciseId()
        );

        validateDuplicateExercise(
                workoutId,
                request.exerciseId()
        );

        validateDisplayOrder(
                workoutId,
                request.displayOrder()
        );

        WorkoutExercise workoutExercise =
                workoutExerciseMapper.toEntity(
                        request,
                        workout,
                        exercise
                );

        WorkoutExercise savedWorkoutExercise =
                workoutExerciseRepository.save(
                        workoutExercise
                );

        log.info(
                "Exercise added to workout successfully. workoutExerciseId={}, workoutId={}, exerciseId={}",
                savedWorkoutExercise.getId(),
                workoutId,
                request.exerciseId()
        );

        return workoutExerciseMapper.toResponse(
                savedWorkoutExercise
        );
    }

    public List<WorkoutExerciseResponse> getWorkoutExercises(
            Long userId,
            Long workoutId
    ) {
        log.debug(
                "Fetching workout exercises. userId={}, workoutId={}",
                userId,
                workoutId
        );

        findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        return workoutExerciseRepository
                .findByWorkoutIdOrderByDisplayOrderAsc(workoutId)
                .stream()
                .map(workoutExerciseMapper::toResponse)
                .toList();
    }

    public WorkoutExerciseResponse getWorkoutExerciseById(
            Long userId,
            Long workoutId,
            Long workoutExerciseId
    ) {
        log.debug(
                "Fetching workout exercise. userId={}, workoutId={}, workoutExerciseId={}",
                userId,
                workoutId,
                workoutExerciseId
        );

        findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findById(workoutExerciseId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Workout exercise not found with id: "
                                                + workoutExerciseId
                                )
                        );

        validateWorkoutExerciseOwnership(
                workoutExercise,
                workoutId
        );

        return workoutExerciseMapper.toResponse(
                workoutExercise
        );
    }

    @Transactional
    public WorkoutExerciseResponse updateWorkoutExercise(
            Long userId,
            Long workoutId,
            Long workoutExerciseId,
            UpdateWorkoutExerciseRequest request
    ) {
        log.info(
                "Updating workout exercise. userId={}, workoutId={}, workoutExerciseId={}",
                userId,
                workoutId,
                workoutExerciseId
        );

        findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findById(workoutExerciseId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Workout exercise not found with id: "
                                                + workoutExerciseId
                                )
                        );

        validateWorkoutExerciseOwnership(
                workoutExercise,
                workoutId
        );

        validateDisplayOrderForUpdate(
                workoutId,
                workoutExercise,
                request.displayOrder()
        );

        workoutExerciseMapper.updateEntity(
                workoutExercise,
                request
        );

        log.info(
                "Workout exercise updated successfully. workoutExerciseId={}",
                workoutExerciseId
        );

        return workoutExerciseMapper.toResponse(
                workoutExercise
        );
    }

    @Transactional
    public void removeExerciseFromWorkout(
            Long userId,
            Long workoutId,
            Long workoutExerciseId
    ) {
        log.info(
                "Removing exercise from workout. userId={}, workoutId={}, workoutExerciseId={}",
                userId,
                workoutId,
                workoutExerciseId
        );

        findWorkoutByIdAndUserId(
                workoutId,
                userId
        );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findById(workoutExerciseId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Workout exercise not found with id: "
                                                + workoutExerciseId
                                )
                        );

        validateWorkoutExerciseOwnership(
                workoutExercise,
                workoutId
        );

        workoutExerciseRepository.delete(
                workoutExercise
        );

        log.info(
                "Workout exercise removed successfully. workoutExerciseId={}",
                workoutExerciseId
        );
    }

    private Workout findWorkoutByIdAndUserId(
            Long workoutId,
            Long userId
    ) {
        return workoutRepository
                .findByIdAndUserId(
                        workoutId,
                        userId
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Workout not found with id: " + workoutId
                        )
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

    private void validateDuplicateExercise(
            Long workoutId,
            Long exerciseId
    ) {
        if (
                workoutExerciseRepository
                        .existsByWorkoutIdAndExerciseId(
                                workoutId,
                                exerciseId
                        )
        ) {
            throw new DuplicateResourceException(
                    "Exercise is already added to this workout"
            );
        }
    }

    private void validateDisplayOrder(
            Long workoutId,
            Integer displayOrder
    ) {
        boolean alreadyUsed = workoutExerciseRepository
                .findByWorkoutIdOrderByDisplayOrderAsc(workoutId)
                .stream()
                .anyMatch(
                        workoutExercise ->
                                workoutExercise.getDisplayOrder()
                                        .equals(displayOrder)
                );

        if (alreadyUsed) {
            throw new DuplicateResourceException(
                    "Display order is already used in this workout: "
                            + displayOrder
            );
        }
    }

    private void validateDisplayOrderForUpdate(
            Long workoutId,
            WorkoutExercise currentWorkoutExercise,
            Integer newDisplayOrder
    ) {
        if (
                currentWorkoutExercise
                        .getDisplayOrder()
                        .equals(newDisplayOrder)
        ) {
            return;
        }

        boolean alreadyUsed = workoutExerciseRepository
                .findByWorkoutIdOrderByDisplayOrderAsc(workoutId)
                .stream()
                .anyMatch(
                        workoutExercise ->
                                !workoutExercise
                                        .getId()
                                        .equals(
                                                currentWorkoutExercise.getId()
                                        )
                                        && workoutExercise
                                        .getDisplayOrder()
                                        .equals(newDisplayOrder)
                );

        if (alreadyUsed) {
            throw new DuplicateResourceException(
                    "Display order is already used in this workout: "
                            + newDisplayOrder
            );
        }
    }

    private void validateWorkoutExerciseOwnership(
            WorkoutExercise workoutExercise,
            Long workoutId
    ) {
        if (
                !workoutExercise
                        .getWorkout()
                        .getId()
                        .equals(workoutId)
        ) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Workout exercise does not belong to this workout",
                    "Workout exercise not found"
            );
        }
    }
}