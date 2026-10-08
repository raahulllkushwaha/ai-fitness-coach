package com.rahul.aifitness.ai.controller;

import com.rahul.aifitness.ai.dto.ChatRequest;
import com.rahul.aifitness.ai.dto.ChatResponse;
import com.rahul.aifitness.ai.service.AiConversationService;
import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiConversationController {

    private final AiConversationService aiConversationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid
            @RequestBody ChatRequest request
    ) {

        Long userId = currentUserService.getCurrentUserId();

        ChatResponse response =
                aiConversationService.chat(
                        userId,
                        request.message()
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}