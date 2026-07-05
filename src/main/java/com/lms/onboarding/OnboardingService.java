package com.lms.onboarding;

import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.platform.EntitlementService;
import com.lms.platform.Plan;
import com.lms.platform.TenantStatus;
import com.lms.security.Roles;
import com.lms.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 학원(기관) 자가 개설 — 신규 테넌트 + 첫 ADMIN 계정을 만든다.
 *
 * <p>테넌트/자격 테이블은 RLS-free라 컨텍스트가 필요 없다. 단, app_user는 RLS 대상이라
 * {@link #createAdmin}은 호출 전에 TenantContext가 새 테넌트로 세팅되어 있어야 한다(컨트롤러가 세팅).
 */
@Service
public class OnboardingService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final EntitlementService entitlementService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public OnboardingService(TenantRepository tenantRepository, AppUserRepository userRepository,
                             EntitlementService entitlementService, PasswordEncoder passwordEncoder,
                             TokenService tokenService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.entitlementService = entitlementService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** 신규 기관 생성 + FREE 요금제 자격 부여. (RLS-free 테이블이라 컨텍스트 불필요) */
    @Transactional
    public Tenant createTenant(String rawOrgCode, String academyName) {
        String org = rawOrgCode == null ? "" : rawOrgCode.trim().toLowerCase();
        if (!org.matches("[a-z0-9-]{2,40}")) {
            throw new BadRequestException("기관 코드는 영소문자·숫자·하이픈 2~40자여야 합니다");
        }
        if (tenantRepository.findByOrgCode(org).isPresent()) {
            throw new ConflictException("이미 사용 중인 기관 코드입니다: " + org);
        }
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant(id, org, academyName.trim(), Plan.FREE, TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        entitlementService.applyPlan(id, Plan.FREE);
        return tenant;
    }

    /** 새 기관의 첫 관리자(ADMIN) 계정 생성 후 자동 로그인 토큰 반환. 호출 전 TenantContext 세팅 필요. */
    @Transactional
    public AuthResponse createAdmin(Tenant tenant, String rawEmail, String password, String displayName) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("비밀번호는 8자 이상이어야 합니다");
        }
        String email = rawEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("이미 가입된 이메일입니다");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(password),
                (displayName == null || displayName.isBlank()) ? null : displayName.trim(), Roles.ADMIN);
        userRepository.save(user);
        List<String> roles = user.roleList();
        String token = tokenService.issue(user.getEmail(), tenant.getId().toString(), roles);
        return new AuthResponse(token, user.getEmail(), tenant.getId().toString(),
                tenant.getOrgCode(), user.getDisplayName(), roles);
    }
}
