package com.rahul.aifitness.integration.intervalsicu;

import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuConnectionStatusResponse;
import com.rahul.aifitness.integration.intervalsicu.dto.IntervalsIcuDisconnectResponse;
import com.rahul.aifitness.integration.intervalsicu.entity.IntervalsIcuConnection;
import com.rahul.aifitness.integration.intervalsicu.repository.IntervalsIcuConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntervalsIcuConnectionService {

    private final IntervalsIcuConnectionRepository connectionRepository;
    private final IntervalsTokenEncryptionService tokenEncryptionService;
    private final IntervalsIcuClient intervalsIcuClient;

    public IntervalsIcuConnectionStatusResponse getStatus(Long userId) {
        return connectionRepository.findByUserId(userId)
                .map(connection ->
                        new IntervalsIcuConnectionStatusResponse(
                                true,
                                connection.getAthleteId(),
                                connection.getScopes(),
                                connection.getConnectedAt(),
                                connection.getLastSyncAt()
                        )
                )
                .orElseGet(() ->
                        new IntervalsIcuConnectionStatusResponse(
                                false,
                                null,
                                null,
                                null,
                                null
                        )
                );
    }

    public IntervalsIcuDisconnectResponse disconnect(Long userId) {
        IntervalsIcuConnection connection =
                connectionRepository.findByUserId(userId).orElse(null);

        if (connection == null) {
            return new IntervalsIcuDisconnectResponse(
                    true,
                    true,
                    "Intervals.icu account was already disconnected"
            );
        }

        boolean remoteRevocationConfirmed = false;

        try {
            String accessToken = tokenEncryptionService.decrypt(
                    connection.getEncryptedAccessToken()
            );

            remoteRevocationConfirmed =
                    intervalsIcuClient.revokeAccessToken(accessToken);

        } catch (RuntimeException exception) {
            log.warn(
                    "Could not confirm remote token revocation for application userId={}",
                    userId
            );
        }

        // Remove the local credential even if the provider is unavailable.
        connectionRepository.delete(connection);

        String message = remoteRevocationConfirmed
                ? "Intervals.icu disconnected and access revocation confirmed"
                : "Disconnected locally, but remote revocation could not be confirmed. Check your Intervals.icu app permissions.";

        return new IntervalsIcuDisconnectResponse(
                true,
                remoteRevocationConfirmed,
                message
        );
    }
}