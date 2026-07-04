package com.lms.platform;

import com.lms.feature.Feature;

import java.util.EnumSet;
import java.util.Set;

/**
 * SaaS 요금제. 각 요금제는 "쓸 수 있는 기능(자격)"의 묶음이다.
 *
 * <p>플랫폼(슈퍼관리자)이 기관에 요금제를 부여하면 그 기능 집합만큼 자격이 생긴다.
 * 기관 ADMIN은 자격 있는 기능 중에서만 실제 on/off(활성화)를 할 수 있다.
 *
 * <p>애드온(요금제 밖 개별 부여)은 이 묶음과 별개로 {@code tenant_entitlement}에 ADDON으로 저장된다.
 */
public enum Plan {

    /** 무료 — 기본 학습(레슨·수강)만. */
    FREE("무료", EnumSet.of(Feature.LESSONS, Feature.ENROLLMENTS)),

    /** 스탠다드 — 학습 + 평가·진단·추천. */
    STANDARD("스탠다드", EnumSet.of(
            Feature.LESSONS, Feature.ENROLLMENTS, Feature.QUIZZES,
            Feature.DIAGNOSIS, Feature.RECOMMENDATIONS)),

    /** 프로 — 전체 모듈(수료증·AI 큐레이션 포함). */
    PRO("프로", EnumSet.allOf(Feature.class));

    private final String displayName;
    private final Set<Feature> features;

    Plan(String displayName, Set<Feature> features) {
        this.displayName = displayName;
        this.features = features;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 이 요금제가 자격을 부여하는 기능 집합. */
    public Set<Feature> features() {
        return features;
    }
}
