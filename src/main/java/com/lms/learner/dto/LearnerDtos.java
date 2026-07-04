package com.lms.learner.dto;

import com.lms.learner.InterestCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** 학습자 프로필·추천 관련 DTO 모음. */
public final class LearnerDtos {

    private LearnerDtos() {
    }

    public record InterestCategoryResponse(String code, String name) {
        public static InterestCategoryResponse from(InterestCategory c) {
            return new InterestCategoryResponse(c.getCode(), c.getName());
        }
    }

    /** 분야별 자가 진단 수준. */
    public record SkillView(
            @NotNull String categoryCode,
            @Min(0) @Max(3) int level
    ) {
    }

    /** 현재 프로필 상태(조회). */
    public record ProfileResponse(
            boolean onboarded,
            List<String> interests,
            List<SkillView> skills
    ) {
    }

    /** 프로필 저장(관심분야 + 자가 진단). 저장 시 onboarded 처리. */
    public record ProfileRequest(
            @NotNull List<String> interests,
            @NotNull List<SkillView> skills
    ) {
    }

    /** 추천 결과 한 건. */
    public record RecommendationResponse(
            UUID courseId,
            String title,
            String categoryCode,
            Integer level,
            int score,
            String reason
    ) {
    }
}
