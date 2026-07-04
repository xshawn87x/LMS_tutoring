package com.lms.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 마켓 판매 콘텐츠 (플랫폼 전역 카탈로그, RLS 없음). */
@Entity
@Table(name = "market_content")
@Getter
@NoArgsConstructor
public class MarketContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String category;

    @Column(nullable = false)
    private int price;

    @Column
    private String provider;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public MarketContent(String title, String description, String category, int price, String provider) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = Math.max(0, price);
        this.provider = provider;
        this.published = true;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(String title, String description, String category, int price, String provider, boolean published) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = Math.max(0, price);
        this.provider = provider;
        this.published = published;
    }
}
