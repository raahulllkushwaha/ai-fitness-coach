package com.rahul.aifitness.integration.telegram.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "telegram_account_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramAccountLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "telegram_username", length = 100)
    private String telegramUsername;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private OffsetDateTime linkedAt;

    public TelegramAccountLink(
            Long userId,
            Long telegramUserId,
            String telegramUsername
    ) {
        this.userId = userId;
        this.telegramUserId = telegramUserId;
        this.telegramUsername = telegramUsername;
    }

    @PrePersist
    protected void onCreate() {
        if (linkedAt == null) {
            linkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}