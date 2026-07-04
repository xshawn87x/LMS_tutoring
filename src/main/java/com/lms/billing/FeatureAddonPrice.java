package com.lms.billing;

import com.lms.feature.Feature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 애드온으로 개별 판매하는 기능의 가격. 여기 등록된 기능만 "판매 가능한 애드온"이다
 * (코어 학습 기능은 미등록 → 요금제 티어로만 차등).
 */
@Entity
@Table(name = "feature_addon_price")
@Getter
@NoArgsConstructor
public class FeatureAddonPrice {

    @Id
    @Enumerated(EnumType.STRING)
    private Feature feature;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false)
    private PricingType pricingType;

    /** FLAT: 월정액. */
    @Column(name = "monthly_price", nullable = false)
    private int monthlyPrice;

    /** USAGE: 단위당 가격. */
    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    /** USAGE: 월 무료 포함량(초과분만 과금). */
    @Column(name = "included_units", nullable = false)
    private int includedUnits;

    /** USAGE: 단위 명칭(예: "분석"). */
    @Column(name = "unit_label")
    private String unitLabel;

    @Column(nullable = false)
    private String currency;

    /** 가격 필드 수정. pricing_type/unit_label은 카탈로그 정의라 고정, 금액·수량만 바꾼다. */
    public void updatePricing(int monthlyPrice, int unitPrice, int includedUnits) {
        this.monthlyPrice = monthlyPrice;
        this.unitPrice = unitPrice;
        this.includedUnits = includedUnits;
    }
}
