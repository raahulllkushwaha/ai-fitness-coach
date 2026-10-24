package com.rahul.aifitness.integration.intervalsicu.dto;

public record IntervalsIcuDisconnectResponse(
        boolean disconnected,
        boolean remoteRevocationConfirmed,
        String message
) {
}