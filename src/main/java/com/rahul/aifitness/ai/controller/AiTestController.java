package com.rahul.aifitness.ai.controller;

import com.rahul.aifitness.ai.service.AiTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiTestService aiTestService;

    @GetMapping("/test")
    public String test(
            @RequestParam(defaultValue = "Say hello to Rahul") String message
    ) {
        return aiTestService.askGemini(message);
    }
}