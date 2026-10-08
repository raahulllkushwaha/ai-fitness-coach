package com.rahul.aifitness.event.kafka;

import com.rahul.aifitness.workout.event.WorkoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkoutEventConsumer {

    private final KafkaIdempotencyService kafkaIdempotencyService;

    @KafkaListener(
            topics = "workout.events",
            groupId = "fitness-coach"
    )
    public void consumeWorkoutEvent(
            WorkoutEvent event
    ) {
        String eventId = event.eventId().toString();

        if (kafkaIdempotencyService.isAlreadyProcessed(eventId)) {

            log.info(
                    "Skipping already processed workout event. eventId={}, eventType={}, workoutId={}",
                    eventId,
                    event.eventType(),
                    event.workoutId()
            );

            return;
        }

        log.info(
                "Processing workout event. eventId={}, eventType={}, workoutId={}, userId={}, occurredAt={}",
                eventId,
                event.eventType(),
                event.workoutId(),
                event.userId(),
                event.occurredAt()
        );

        kafkaIdempotencyService.markAsProcessed(eventId);

        log.info(
                "Workout event processed and marked as processed. eventId={}",
                eventId
        );
    }
}