package com.rahul.aifitness.integration.intervalsicu.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntervalsActivityDto(

        @JsonProperty("id")
        String id,

        @JsonProperty("name")
        String name,

        @JsonProperty("type")
        String type,

        @JsonProperty("start_date")
        OffsetDateTime startDate,

        @JsonProperty("start_date_local")
        LocalDateTime startDateLocal,

        @JsonProperty("moving_time")
        Integer movingTimeSeconds,

        @JsonProperty("elapsed_time")
        Integer elapsedTimeSeconds,

        @JsonProperty("distance")
        Double distanceMeters,

        @JsonProperty("total_elevation_gain")
        @JsonAlias("elevation_gain")
        Double elevationGainMeters,

        @JsonProperty("average_heartrate")
        @JsonAlias("average_heart_rate")
        Double averageHeartRate,

        @JsonProperty("max_heartrate")
        @JsonAlias("max_heart_rate")
        Double maxHeartRate,

        @JsonProperty("calories")
        Double calories

) {
}