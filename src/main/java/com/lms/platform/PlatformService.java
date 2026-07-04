package com.lms.platform;

import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.error.UnauthorizedException;
import com.lms.feature.Feature;
import com.lms.platform.dto.PlatformDtos.EntitlementView;
import com.lms.platform.dto.PlatformDtos.PlanView;
import com.lms.platform.dto.PlatformDtos.TenantView;
import com.lms.security.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 플랫폼(슈퍼관리자) 오케스트레이션: 로그인, 테넌트별 요금제·자격 조회/변경.
 * 실제 자격 규칙은 {@link EntitlementService}에 위임하고, 여기서는 인증·조합·뷰 변환을 담당한다.
 */
@Service
public class PlatformService {

    private final TenantRepository tenantRepository;
    private final TenantEntitlementRepository entitlementRepository;
    private final EntitlementService entitlementService;
    private final TokenService tokenService;
    private final com.lms.platform.audit.AuditService auditService;

    // 로컬 부트스트랩용 슈퍼관리자 자격증명(설정/환경변수 주입). 운영에선 별도 IdP/시크릿으로 대체.
    private final String adminEmail;
    private final String adminPassword;

    public PlatformService(TenantRepository tenantRepository,
                           TenantEntitlementRepository entitlementRepository,
                           EntitlementService entitlementService,
                           TokenService tokenService,
                           com.lms.platform.audit.AuditService auditService,
                           @Value("${platform.admin.email}") String adminEmail,
                           @Value("${platform.admin.password}") String adminPassword) {
        this.tenantRepository = tenantRepository;
        this.entitlementRepository = entitlementRepository;
        this.entitlementService = entitlementService;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /** 설정 부트스트랩 자격증명으로 검증하고 플랫폼 토큰을 발급. 틀리면 401. */
    public String login(String email, String password) {
        boolean ok = adminEmail.equalsIgnoreCase(email.trim()) && adminPassword.equals(password);
        if (!ok) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        return tokenService.issuePlatform(adminEmail);
    }

    /** 모든 기관의 요금제 + 기능별 자격 매트릭스. */
    @Transactional(readOnly = true)
    public List<TenantView> listTenants() {
        return tenantRepository.findAll().stream()
                .sorted((a, b) -> a.getOrgCode().compareToIgnoreCase(b.getOrgCode()))
                .map(this::toView)
                .toList();
    }

    /** 기관 요금제 변경 → PLAN 자격 교체(ADDON 보존). */
    @Transactional
    public TenantView changePlan(UUID tenantId, String planName) {
        Plan plan = parsePlan(planName);
        entitlementService.applyPlan(tenantId, plan);
        auditService.record("PLAN_CHANGE", "TENANT", tenantId.toString(), "요금제 → " + plan.name());
        return toView(requireTenant(tenantId));
    }

    /** 애드온(요금제 밖 개별 기능) 자격 부여. */
    @Transactional
    public TenantView grantAddon(UUID tenantId, Feature feature) {
        requireTenant(tenantId);
        entitlementService.grantAddon(tenantId, feature);
        auditService.record("ENTITLEMENT_GRANT", "TENANT", tenantId.toString(), "애드온 부여 · " + feature.name());
        return toView(requireTenant(tenantId));
    }

    /** 자격 회수(PLAN/ADDON 무관). */
    @Transactional
    public TenantView revoke(UUID tenantId, Feature feature) {
        requireTenant(tenantId);
        entitlementService.revokeEntitlement(tenantId, feature);
        auditService.record("ENTITLEMENT_REVOKE", "TENANT", tenantId.toString(), "자격 회수 · " + feature.name());
        return toView(requireTenant(tenantId));
    }

    /** 요금제 카탈로그(각 요금제가 포함하는 기능). */
    public List<PlanView> plans() {
        return Arrays.stream(Plan.values())
                .map(p -> new PlanView(
                        p.name(), p.getDisplayName(),
                        p.features().stream().map(Feature::name).toList()))
                .toList();
    }

    // --- 내부 ---

    private TenantView toView(Tenant tenant) {
        // tenant_entitlement는 RLS-free → 반드시 tenant_id로 스코프해서 읽는다.
        Map<Feature, EntitlementSource> sources = entitlementRepository.findByTenantId(tenant.getId()).stream()
                .collect(Collectors.toMap(TenantEntitlement::getFeature, TenantEntitlement::getSource,
                        (a, b) -> a));
        List<EntitlementView> features = Arrays.stream(Feature.values())
                .map(f -> {
                    EntitlementSource src = sources.get(f);
                    return new EntitlementView(
                            f.name(), f.getDisplayName(),
                            src != null, src == null ? null : src.name(), f.isImplemented());
                })
                .toList();
        return new TenantView(tenant.getId(), tenant.getOrgCode(), tenant.getName(),
                tenant.getPlan().name(), tenant.getStatus().name(), features);
    }

    /** 단일 기관 뷰(상태 변경 후 응답용). */
    @Transactional(readOnly = true)
    public TenantView tenantView(UUID tenantId) {
        return toView(requireTenant(tenantId));
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("기관을 찾을 수 없습니다: " + tenantId));
    }

    private Plan parsePlan(String name) {
        try {
            return Plan.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("알 수 없는 요금제입니다: " + name);
        }
    }
}
