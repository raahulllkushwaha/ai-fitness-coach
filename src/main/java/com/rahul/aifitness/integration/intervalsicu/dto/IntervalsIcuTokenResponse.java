package com.rahul.aifitness.integration.intervalsicu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IntervalsIcuTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("athlete")
        Athlete athlete
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Athlete(
            @JsonProperty("id")
            String id,

            @JsonProperty("name")
            String name
    ) {
    }
}