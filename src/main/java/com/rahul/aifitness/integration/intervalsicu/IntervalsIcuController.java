package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.auth.service.CurrentUserService;
import com.rahul.aifitness.common.response.ApiResponse;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuAuthorizeUrlResponse;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuConnectionStatusResponse;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuDisconnectResponse;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/integrations/intervals-icu")
@RequiredArgsConstructor
@Slf4j
public class IntervalsIcuController {

    private final CurrentUserService currentUserService;
    private final IntervalsIcuSyncService syncService;
    private final IntervalsIcuOAuthStateService stateService;
    private final IntervalsIcuOAuthService oauthService;
    private final IntervalsIcuConnectionService connectionService;

    @Value("${INTERVALS_OAUTH_FRONTEND_REDIRECT_URI:https://ai-fitness-coach-frontend.pages.dev/integrations}")
    private String frontendRedirectUri;

    @GetMapping("/authorize-url")
    public ResponseEntity<ApiResponse<IntervalsIcuAuthorizeUrlResponse>>
    authorizeUrl() {
        Long userId = currentUserService.getCurrentUserId();

        String authorizationUrl =
                stateService.createAuthorizationUrl(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Intervals.icu authorization URL created",
                        new IntervalsIcuAuthorizeUrlResponse(
                                authorizationUrl
                        )
                )
        );
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (StringUtils.hasText(error)) {
            return redirectToFrontend("denied");
        }

        try {
            oauthService.completeAuthorization(code, state);
            return redirectToFrontend("connected");

        } catch (RuntimeException exception) {
            log.warn(
                    "Intervals.icu OAuth callback failed: {}",
                    exception.getClass().getSimpleName()
            );

            return redirectToFrontend("error");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String outcome) {
        URI location = UriComponentsBuilder
                .fromUriString(frontendRedirectUri)
                .queryParam("intervals", outcome)
                .build()
                .encode()
                .toUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<IntervalsIcuConnectionStatusResponse>>
    status() {
        Long userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Intervals.icu connection status retrieved",
                        connectionService.getStatus(userId)
                )
        );
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<IntervalsIcuDisconnectResponse>>
    disconnect() {
        Long userId = currentUserService.getCurrentUserId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Intervals.icu disconnect processed",
                        connectionService.disconnect(userId)
                )
        );
    }

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