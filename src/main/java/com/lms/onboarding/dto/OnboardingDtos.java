package com.lms.onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 학원(기관) 자가 개설 요청 — 기관 + 첫 관리자 계정을 한 번에 만든다. */
public final class OnboardingDtos {

    private OnboardingDtos() {
    }

    public record AcademySignupRequest(
            @NotBlank String orgCode,
            @NotBlank String academyName,
            @NotBlank @Email String adminEmail,
            @NotBlank String adminPassword,
            String adminName) {
    }
}
