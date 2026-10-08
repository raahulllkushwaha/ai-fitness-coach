package com.rahul.aifitness.event.kafka;

import com.rahul.aifitness.workout.event.WorkoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkoutEventConsumer {

    @KafkaListener(
            topics = "workout.events",
            groupId = "fitness-coach"
    )
    public void consumeWorkoutEvent(
            WorkoutEvent event
    ) {
        log.info(
                "Workout event consumed successfully. " +
                        "eventId={}, eventType={}, workoutId={}, userId={}, occurredAt={}",
                event.eventId(),
                event.eventType(),
                event.workoutId(),
                event.userId(),
                event.occurredAt()
        );


    }
}