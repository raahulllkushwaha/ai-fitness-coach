package com.rahul.aifitness.workout.mapper;

import com.rahul.aifitness.user.entity.User;
import com.rahul.aifitness.workout.dto.request.CreateWorkoutRequest;
import com.rahul.aifitness.workout.dto.request.UpdateWorkoutRequest;
import com.rahul.aifitness.workout.dto.response.WorkoutResponse;
import com.rahul.aifitness.workout.entity.Workout;
import org.springframework.stereotype.Component;

@Component
public class WorkoutMapper {

    public Workout toEntity(
            CreateWorkoutRequest request,
            User user
    ) {
        return Workout.builder()
                .user(user)
                .workoutType(request.workoutType())
                .startedAt(request.startedAt())
                .endedAt(request.endedAt())
                .durationSeconds(request.durationSeconds())
                .distanceMeters(request.distanceMeters())
                .elevationGainMeters(request.elevationGainMeters())
                .averageHeartRate(request.averageHeartRate())
                .maxHeartRate(request.maxHeartRate())
                .calories(request.calories())
                .averagePaceSecondsPerKm(request.averagePaceSecondsPerKm())
                .source(request.source())
                .externalId(request.externalId())
                .notes(request.notes())
                .build();
    }

    public WorkoutResponse toResponse(Workout workout) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getUser().getId(),
                workout.getWorkoutType(),
                workout.getStartedAt(),
                workout.getEndedAt(),
                workout.getDurationSeconds(),
                workout.getDistanceMeters(),
                workout.getElevationGainMeters(),
                workout.getAverageHeartRate(),
                workout.getMaxHeartRate(),
                workout.getCalories(),
                workout.getAveragePaceSecondsPerKm(),
                workout.getSource(),
                workout.getExternalId(),
                workout.getNotes(),
                workout.getCreatedAt(),
                workout.getUpdatedAt()
        );
    }

    public void updateEntity(
            Workout workout,
            UpdateWorkoutRequest request
    ) {
        workout.updateWorkout(
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
                request.externalId(),
                request.notes()
        );
    }
}