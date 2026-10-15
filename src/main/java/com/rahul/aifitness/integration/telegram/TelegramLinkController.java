package com.rahul.aifitness.integration.telegram;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.integration.telegram.dto.TelegramLinkCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/telegram")
@RequiredArgsConstructor
public class TelegramLinkController {

    private final CurrentUserService currentUserService;
    private final TelegramLinkService telegramLinkService;

    @PostMapping("/link-code")
    public ResponseEntity<ApiResponse<TelegramLinkCodeResponse>>
    generateLinkCode() {

        Long userId = currentUserService.getCurrentUserId();

        TelegramLinkCodeResponse response =
                telegramLinkService.generateLinkCode(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Telegram linking code generated",
                        response
                )
        );
    }
}