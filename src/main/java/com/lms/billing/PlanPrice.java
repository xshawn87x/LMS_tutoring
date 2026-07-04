package com.lms.billing;

import com.lms.platform.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 요금제 월정액. RLS 없는 전역 가격 카탈로그. */
@Entity
@Table(name = "plan_price")
@Getter
@NoArgsConstructor
public class PlanPrice {

    @Id
    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Column(name = "monthly_price", nullable = false)
    private int monthlyPrice;

    @Column(nullable = false)
    private String currency;

    public void setMonthlyPrice(int monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }
}
