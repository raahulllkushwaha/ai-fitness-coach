package com.rahul.aifitness.integration.intervalsicu.dto;

import java.time.OffsetDateTime;

public record IntervalsIcuConnectionStatusResponse(
        boolean connected,
        String athleteId,
        String scopes,
        OffsetDateTime connectedAt,
        OffsetDateTime lastSyncAt
) {
}