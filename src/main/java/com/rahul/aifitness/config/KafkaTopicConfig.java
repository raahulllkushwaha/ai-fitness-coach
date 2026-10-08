package com.rahul.aifitness.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String WORKOUT_EVENTS_TOPIC = "workout.events";

    @Bean
    public NewTopic workoutEventsTopic() {
        return TopicBuilder
                .name(WORKOUT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}