package com.rahul.aifitness.ai.controller;

import com.rahul.aifitness.ai.dto.CoachRecommendation;
import com.rahul.aifitness.ai.service.AiCoachService;
import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiCoachController {

    private final AiCoachService aiCoachService;
    private final CurrentUserService currentUserService;

    @GetMapping("/coach")
    public ResponseEntity<ApiResponse<CoachRecommendation>> getCoachRecommendation(
            @RequestParam OffsetDateTime start,
            @RequestParam OffsetDateTime end
    ) {

        Long userId = currentUserService.getCurrentUserId();

        CoachRecommendation recommendation =
                aiCoachService.generateRecommendation(
                        userId,
                        start,
                        end
                );

        return ResponseEntity.ok(
                ApiResponse.success(recommendation)
        );
    }
}