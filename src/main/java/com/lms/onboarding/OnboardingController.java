package com.lms.onboarding;

import com.lms.auth.Tenant;
import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.onboarding.dto.OnboardingDtos.AcademySignupRequest;
import com.lms.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학원(기관) 자가 개설 — 공개 엔드포인트. 기관 + 첫 관리자를 만들고 자동 로그인 토큰을 돌려준다.
 *
 * <p>흐름: 기관/자격 생성(RLS-free) → TenantContext 세팅 → 관리자 계정 생성(app_user RLS).
 * AuthController.register와 동일한 컨텍스트 처리 패턴.
 */
@RestController
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) {
        this.service = service;
    }

    @PostMapping("/api/onboarding/academy")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse academy(@Valid @RequestBody AcademySignupRequest req) {
        Tenant tenant = service.createTenant(req.orgCode(), req.academyName());
        TenantContext.set(tenant.getId());
        return service.createAdmin(tenant, req.adminEmail(), req.adminPassword(), req.adminName());
    }
}
