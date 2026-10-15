package com.rahul.aifitness.integration.telegram.repository;

import com.rahul.aifitness.integration.telegram.entity.TelegramAccountLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelegramAccountLinkRepository
        extends JpaRepository<TelegramAccountLink, Long> {

    Optional<TelegramAccountLink> findByTelegramUserId(Long telegramUserId);

    Optional<TelegramAccountLink> findByUserId(Long userId);
}