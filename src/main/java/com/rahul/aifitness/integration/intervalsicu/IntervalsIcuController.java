package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/integrations/intervals-icu")
@RequiredArgsConstructor
public class IntervalsIcuController {

    private final CurrentUserService currentUserService;
    private final IntervalsIcuSyncService syncService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<IntervalsIcuSyncResponse>> sync(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate oldest,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate newest,

            @RequestParam(defaultValue = "200")
            int limit
    ) {
        Long userId = currentUserService.getCurrentUserId();

        IntervalsIcuSyncResponse response =
                syncService.syncActivities(
                        userId,
                        oldest,
                        newest,
                        limit
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Intervals.icu activity sync completed",
                        response
                )
        );
    }
}