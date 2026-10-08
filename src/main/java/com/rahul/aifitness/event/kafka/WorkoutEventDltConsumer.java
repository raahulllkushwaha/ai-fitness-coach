package com.rahul.aifitness.event.kafka;

import com.rahul.aifitness.workout.event.WorkoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkoutEventDltConsumer {

    @KafkaListener(
            topics = "workout.events-dlt",
            groupId = "fitness-coach-dlt"
    )
    public void consumeDeadLetterEvent(
            WorkoutEvent event
    ) {
        log.error(
                "Workout event moved to DLT. " +
                        "eventId={}, eventType={}, workoutId={}, userId={}, occurredAt={}",
                event.eventId(),
                event.eventType(),
                event.workoutId(),
                event.userId(),
                event.occurredAt()
        );
    }
}