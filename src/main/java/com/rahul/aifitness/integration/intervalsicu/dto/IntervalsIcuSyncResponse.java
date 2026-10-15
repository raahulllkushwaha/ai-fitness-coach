package com.rahul.aifitness.integration.intervalsicu.dto;

import java.time.LocalDate;

public record IntervalsIcuSyncResponse(
        LocalDate oldest,
        LocalDate newest,
        int fetchedActivities,
        int importedWorkouts,
        int skippedDuplicates,
        int skippedInvalidActivities
) {
}