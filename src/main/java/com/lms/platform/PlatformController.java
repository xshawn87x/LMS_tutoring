package com.lms.platform;

import com.lms.billing.BillingService;
import com.lms.billing.dto.BillingDtos.AddonPriceUpdateRequest;
import com.lms.billing.dto.BillingDtos.AddonPriceView;
import com.lms.billing.dto.BillingDtos.InvoiceView;
import com.lms.billing.dto.BillingDtos.IssueInvoiceRequest;
import com.lms.billing.dto.BillingDtos.PlanPriceUpdateRequest;
import com.lms.billing.dto.BillingDtos.PlanPriceView;
import com.lms.billing.dto.BillingDtos.PricingView;
import com.lms.billing.dto.BillingDtos.StatementView;
import com.lms.billing.dto.BillingDtos.TenantBillingView;
import com.lms.feature.Feature;
import com.lms.platform.Plan;
import com.lms.platform.audit.AuditService;
import com.lms.platform.audit.AuditView;
import com.lms.platform.dto.PlatformDtos.PlanRequest;
import com.lms.platform.dto.PlatformDtos.PlanView;
import com.lms.platform.dto.PlatformDtos.PlatformLoginRequest;
import com.lms.platform.dto.PlatformDtos.PlatformLoginResponse;
import com.lms.platform.dto.PlatformDtos.TenantView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 플랫폼(SaaS 제공자) 슈퍼관리자 API — 테넌트 경계를 넘어 요금제·자격을 관리한다.
 *
 * <p>로그인만 공개, 나머지는 PLATFORM_ADMIN 전용. 이 토큰은 tenant_id가 없어 RLS 대상 테이블에는
 * 접근하지 못하고, 전역 테이블(tenant / tenant_entitlement)만 다룬다.
 */
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final PlatformService service;
    private final BillingService billingService;
    private final AuditService auditService;
    private final AccountStandingService accountStandingService;
    private final PlatformAnalyticsService analyticsService;

    public PlatformController(PlatformService service, BillingService billingService,
                             AuditService auditService, AccountStandingService accountStandingService,
                             PlatformAnalyticsService analyticsService) {
        this.service = service;
        this.billingService = billingService;
        this.auditService = auditService;
        this.accountStandingService = accountStandingService;
        this.analyticsService = analyticsService;
    }

    /** 플랫폼 애널리틱스 — 매출 추이·MRR·플랜 분포·이탈·학원별 KPI. */
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PlatformAnalyticsService.PlatformAnalytics analytics() {
        return analyticsService.analytics();
    }

    /** 슈퍼관리자 로그인 (공개). 설정 부트스트랩 계정 검증 → 플랫폼 토큰 발급. */
    @PostMapping("/login")
    public PlatformLoginResponse login(@Valid @RequestBody PlatformLoginRequest request) {
        String token = service.login(request.email(), request.password());
        return new PlatformLoginResponse(token, request.email().trim());
    }

    /** 요금제 카탈로그(각 요금제 포함 기능). */
    @GetMapping("/plans")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<PlanView> plans() {
        return service.plans();
    }

    /** 모든 기관 + 기능별 자격 매트릭스. */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<TenantView> tenants() {
        return service.listTenants();
    }

    /** 기관 요금제 변경. */
    @PutMapping("/tenants/{tenantId}/plan")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantView changePlan(@PathVariable UUID tenantId, @Valid @RequestBody PlanRequest request) {
        return service.changePlan(tenantId, request.plan());
    }

    /** 애드온 자격 부여(요금제 밖 개별 기능). */
    @PostMapping("/tenants/{tenantId}/entitlements/{feature}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantView grant(@PathVariable UUID tenantId, @PathVariable Feature feature) {
        return service.grantAddon(tenantId, feature);
    }

    /** 자격 회수(PLAN/ADDON 무관). */
    @DeleteMapping("/tenants/{tenantId}/entitlements/{feature}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantView revoke(@PathVariable UUID tenantId, @PathVariable Feature feature) {
        return service.revoke(tenantId, feature);
    }

    // --- 가격 / 청구 ---

    /** 가격 카탈로그(요금제 월정액 + 애드온 가격). */
    @GetMapping("/pricing")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PricingView pricing() {
        return new PricingView(
                billingService.planPrices().stream().map(PlanPriceView::from).toList(),
                billingService.addonPrices().stream().map(AddonPriceView::from).toList());
    }

    /** 한 기관의 이번 달 청구 명세 + 인보이스 이력. */
    @GetMapping("/tenants/{tenantId}/billing")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantBillingView billing(@PathVariable UUID tenantId) {
        StatementView statement = StatementView.from(billingService.currentStatement(tenantId));
        List<InvoiceView> invoices = billingService.invoices(tenantId).stream().map(InvoiceView::from).toList();
        return new TenantBillingView(statement, invoices);
    }

    /** 인보이스 발행(기본: 이번 달 명세를 스냅샷). */
    @PostMapping("/tenants/{tenantId}/invoices")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public InvoiceView issueInvoice(@PathVariable UUID tenantId, @RequestBody(required = false) IssueInvoiceRequest request) {
        String period = (request == null || request.period() == null || request.period().isBlank())
                ? com.lms.billing.BillingPeriods.current() : request.period().trim();
        return InvoiceView.from(billingService.issueInvoice(tenantId, period));
    }

    /** 인보이스 결제(모의 결제 프로바이더). */
    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public InvoiceView payInvoice(@PathVariable UUID invoiceId) {
        return InvoiceView.from(billingService.payInvoice(invoiceId));
    }

    /** 요금제 월정액 수정. */
    @PutMapping("/pricing/plans/{plan}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PlanPriceView updatePlanPrice(@PathVariable Plan plan, @RequestBody PlanPriceUpdateRequest request) {
        return PlanPriceView.from(billingService.updatePlanPrice(plan, request.monthlyPrice()));
    }

    /** 애드온 가격 수정. */
    @PutMapping("/pricing/addons/{feature}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public AddonPriceView updateAddonPrice(@PathVariable Feature feature, @RequestBody AddonPriceUpdateRequest request) {
        return AddonPriceView.from(billingService.updateAddonPrice(
                feature, request.monthlyPrice(), request.unitPrice(), request.includedUnits()));
    }

    // --- 기관 상태(정지/해제) + 청구 마감 ---

    /** 기관 수동 정지. */
    @PostMapping("/tenants/{tenantId}/suspend")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantView suspend(@PathVariable UUID tenantId) {
        accountStandingService.suspend(tenantId);
        return service.tenantView(tenantId);
    }

    /** 기관 이용 재개(정지/연체 해제). */
    @PostMapping("/tenants/{tenantId}/reactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantView reactivate(@PathVariable UUID tenantId) {
        accountStandingService.reactivate(tenantId);
        return service.tenantView(tenantId);
    }

    /** 청구 마감(수동 트리거). 해당 월의 인보이스를 모든 기관에 발행하고 연체를 판정한다. */
    @PostMapping("/billing/close-period")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<InvoiceView> closePeriod(@RequestBody IssueInvoiceRequest request) {
        String period = (request == null || request.period() == null || request.period().isBlank())
                ? com.lms.billing.BillingPeriods.current() : request.period().trim();
        return billingService.closePeriod(period).stream().map(InvoiceView::from).toList();
    }

    // --- 감사 로그 ---

    /** 최근 감사 로그(플랫폼 전역). */
    @GetMapping("/audit")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<AuditView> audit() {
        return auditService.recent(100).stream().map(AuditView::from).toList();
    }

    /** 특정 기관 관련 감사 로그. */
    @GetMapping("/tenants/{tenantId}/audit")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<AuditView> tenantAudit(@PathVariable UUID tenantId) {
        return auditService.forTarget(tenantId.toString(), 50).stream().map(AuditView::from).toList();
    }
}
