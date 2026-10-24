package com.rahul.aifitness.integration.intervalsicu.repository;

import com.rahul.aifitness.integration.intervalsicu.entity.IntervalsIcuConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntervalsIcuConnectionRepository
        extends JpaRepository<IntervalsIcuConnection, Long> {

    Optional<IntervalsIcuConnection> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}