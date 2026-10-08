package com.rahul.aifitness.workout.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkoutEvent(
        UUID eventId,
        WorkoutEventType eventType,
        Long workoutId,
        Long userId,
        OffsetDateTime occurredAt
) {
}