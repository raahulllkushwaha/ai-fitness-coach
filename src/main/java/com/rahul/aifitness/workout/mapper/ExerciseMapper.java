package com.rahul.aifitness.workout.mapper;

import com.rahul.aifitness.workout.dto.request.CreateExerciseRequest;
import com.rahul.aifitness.workout.dto.request.UpdateExerciseRequest;
import com.rahul.aifitness.workout.dto.response.ExerciseResponse;
import com.rahul.aifitness.workout.entity.Exercise;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ExerciseMapper {

    public Exercise toEntity(CreateExerciseRequest request) {
        String name = normalizeName(request.name());

        return Exercise.builder()
                .name(request.name().trim())
                .normalizedName(name)
                .build();
    }

    public ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getNormalizedName(),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt()
        );
    }

    public void updateEntity(
            Exercise exercise,
            UpdateExerciseRequest request
    ) {
        String name = request.name().trim();

        exercise.updateName(
                name,
                normalizeName(name)
        );
    }

    private String normalizeName(String name) {
        return name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}