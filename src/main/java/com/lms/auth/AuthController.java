package com.lms.auth;

import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.auth.dto.AuthDtos.LoginRequest;
import com.lms.auth.dto.AuthDtos.PasswordResetConfirm;
import com.lms.auth.dto.AuthDtos.PasswordResetRequest;
import com.lms.auth.dto.AuthDtos.PasswordResetTokenResponse;
import com.lms.auth.dto.AuthDtos.RegisterRequest;
import com.lms.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입/로그인 API (공개 엔드포인트). dev 토큰을 대체하는 실제 인증 경로.
 *
 * <p>흐름: org_code로 테넌트를 먼저 조회(별도 트랜잭션) → TenantContext 세팅 →
 * app_user 작업(해당 테넌트로 RLS 격리). TenantFilter가 요청 종료 시 컨텍스트를 정리한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        Tenant tenant = authService.resolveTenant(request.orgCode());
        TenantContext.set(tenant.getId());
        return authService.register(tenant, request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Tenant tenant = authService.resolveTenant(request.orgCode());
        TenantContext.set(tenant.getId());
        return authService.login(tenant, request);
    }

    /** 비밀번호 재설정 토큰 발급(로컬: 토큰을 응답에 포함). */
    @PostMapping("/password-reset/request")
    public PasswordResetTokenResponse requestReset(@Valid @RequestBody PasswordResetRequest request) {
        Tenant tenant = authService.resolveTenant(request.orgCode());
        TenantContext.set(tenant.getId());
        String token = authService.requestPasswordReset(request.email());
        return new PasswordResetTokenResponse(token, "재설정 토큰이 발급되었습니다(로컬: 응답에 포함). 1시간 내 사용하세요.");
    }

    /** 토큰으로 새 비밀번호 확정. */
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReset(@Valid @RequestBody PasswordResetConfirm request) {
        Tenant tenant = authService.resolveTenant(request.orgCode());
        TenantContext.set(tenant.getId());
        authService.confirmPasswordReset(request);
    }
}
