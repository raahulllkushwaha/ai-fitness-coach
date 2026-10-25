package com.rahul.aifitness.integration.intervalsicu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntervalsCalendarEventDto(

        @JsonProperty("id")
        Long id,

        @JsonProperty("name")
        String name,

        @JsonProperty("category")
        String category,

        @JsonProperty("type")
        String type,

        @JsonProperty("start_date_local")
        String startDateLocal,

        @JsonProperty("end_date_local")
        String endDateLocal,

        @JsonProperty("description")
        String description,

        @JsonProperty("moving_time")
        Integer movingTimeSeconds,

        @JsonProperty("distance")
        Double distanceMeters,

        @JsonProperty("icu_training_load")
        Double trainingLoad,

        @JsonProperty("time_target")
        Integer timeTargetSeconds,

        @JsonProperty("distance_target")
        Double distanceTargetMeters

) {
}