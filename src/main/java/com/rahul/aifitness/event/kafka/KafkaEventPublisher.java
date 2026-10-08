package com.rahul.aifitness.event.kafka;

import com.rahul.aifitness.config.KafkaTopicConfig;
import com.rahul.aifitness.workout.event.WorkoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, WorkoutEvent> kafkaTemplate;

    public void publishWorkoutEvent(WorkoutEvent event) {

        String key = event.workoutId().toString();

        kafkaTemplate
                .send(
                        KafkaTopicConfig.WORKOUT_EVENTS_TOPIC,
                        key,
                        event
                )
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.error(
                                "Failed to publish workout event. eventId={}, eventType={}, workoutId={}",
                                event.eventId(),
                                event.eventType(),
                                event.workoutId(),
                                exception
                        );
                        return;
                    }

                    log.info(
                            "Workout event published successfully. eventId={}, eventType={}, workoutId={}, partition={}, offset={}",
                            event.eventId(),
                            event.eventType(),
                            event.workoutId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}