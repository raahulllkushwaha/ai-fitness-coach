package com.rahul.aifitness.integration.intervalsicu.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "intervals_icu_connections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IntervalsIcuConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(
            name = "access_token_encrypted",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String encryptedAccessToken;

    @Column(name = "scopes", length = 500)
    private String scopes;

    @Column(name = "athlete_id", length = 100)
    private String athleteId;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public IntervalsIcuConnection(
            Long userId,
            String encryptedAccessToken,
            String scopes,
            String athleteId
    ) {
        this.userId = userId;
        this.encryptedAccessToken = encryptedAccessToken;
        this.scopes = scopes;
        this.athleteId = athleteId;
    }

    public void updateCredentials(
            String encryptedAccessToken,
            String scopes,
            String athleteId
    ) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.scopes = scopes;
        this.athleteId = athleteId;
        this.connectedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void markSynced() {
        this.lastSyncAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (connectedAt == null) {
            connectedAt = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}