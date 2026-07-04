package com.lms.platform.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/** 플랫폼(슈퍼관리자) API의 요청/응답 DTO 모음. */
public final class PlatformDtos {

    private PlatformDtos() {
    }

    /** 슈퍼관리자 로그인. 테넌트 개념이 없다(설정 부트스트랩 계정). */
    public record PlatformLoginRequest(
            @NotBlank String email,
            @NotBlank String password) {
    }

    public record PlatformLoginResponse(String token, String email) {
    }

    /** 요금제 변경 요청. plan = FREE | STANDARD | PRO. */
    public record PlanRequest(@NotBlank String plan) {
    }

    /** 한 기능의 자격 상태(테넌트 기준). source는 자격 있을 때만(PLAN|ADDON), 없으면 null. */
    public record EntitlementView(
            String feature,
            String displayName,
            boolean entitled,
            String source,
            boolean implemented) {
    }

    /** 한 기관의 요금제 + 이용 상태 + 전체 기능별 자격 매트릭스. */
    public record TenantView(
            UUID id,
            String orgCode,
            String name,
            String plan,
            String status,
            List<EntitlementView> features) {
    }

    /** 요금제 카탈로그 항목(어떤 기능을 포함하는지). */
    public record PlanView(String name, String displayName, List<String> features) {
    }
}
