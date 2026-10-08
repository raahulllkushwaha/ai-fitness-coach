package com.rahul.aifitness.event.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KafkaIdempotencyService {

    private static final String PROCESSED_EVENT_PREFIX =
            "kafka:processed-event:";

    private static final Duration EVENT_TTL =
            Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isAlreadyProcessed(String eventId) {
        String key = buildKey(eventId);

        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(key)
        );
    }

    public void markAsProcessed(String eventId) {
        String key = buildKey(eventId);

        stringRedisTemplate.opsForValue().set(
                key,
                "processed",
                EVENT_TTL
        );
    }

    private String buildKey(String eventId) {
        return PROCESSED_EVENT_PREFIX + eventId;
    }
}