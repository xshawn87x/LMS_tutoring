package com.lms.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarketContentRepository extends JpaRepository<MarketContent, UUID> {
    List<MarketContent> findByPublishedTrueOrderByCreatedAtDesc();

    List<MarketContent> findAllByOrderByCreatedAtDesc();
}
