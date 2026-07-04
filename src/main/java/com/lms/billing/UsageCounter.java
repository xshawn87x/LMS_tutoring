package com.lms.billing;

import com.lms.feature.Feature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** 한 테넌트의 한 기능에 대한 월별 사용량 집계. RLS-free(전역) → tenant_id 명시 스코프. */
@Entity
@Table(name = "usage_counter")
@Getter
@NoArgsConstructor
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Feature feature;

    /** 'YYYY-MM' */
    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private int count;

    public UsageCounter(UUID tenantId, Feature feature, String period) {
        this.tenantId = tenantId;
        this.feature = feature;
        this.period = period;
        this.count = 0;
    }

    public void increment(int by) {
        this.count += by;
    }
}
