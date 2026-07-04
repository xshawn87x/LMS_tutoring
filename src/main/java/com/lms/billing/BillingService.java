package com.lms.billing;

import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.platform.EntitlementSource;
import com.lms.platform.Plan;
import com.lms.platform.TenantEntitlement;
import com.lms.platform.TenantEntitlementRepository;
import com.lms.platform.TenantStatus;
import com.lms.platform.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 청구 계층: 요금제 + 애드온(정액) + 사용량으로 월 청구 명세를 계산하고, 인보이스를 발행/결제한다.
 *
 * <p>과금 규칙:
 * <ul>
 *   <li>요금제: {@code plan_price}의 월정액.
 *   <li>정액 애드온: <b>ADDON</b>으로 부여된 기능 중 {@code feature_addon_price}가 FLAT인 것(요금제에 포함된
 *       PLAN 기능은 요금제가에 이미 포함되므로 별도 과금하지 않음).
 *   <li>사용량: 자격이 있는 USAGE 가격 기능(예: AI 분석)을, 무료 포함량 초과분만 단가로 과금(획득 경로 무관).
 * </ul>
 */
@Service
@Transactional
public class BillingService {

    private static final String CURRENCY = "KRW";
    /** 부가가치세율(10%). 명세 소계에 부가세 라인을 더해 총액을 만든다. */
    private static final double VAT_RATE = 0.10;

    private final PlanPriceRepository planPriceRepository;
    private final FeatureAddonPriceRepository addonPriceRepository;
    private final UsageService usageService;
    private final TenantEntitlementRepository entitlementRepository;
    private final TenantRepository tenantRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentProvider paymentProvider;
    private final AuditService auditService;

    public BillingService(PlanPriceRepository planPriceRepository,
                          FeatureAddonPriceRepository addonPriceRepository,
                          UsageService usageService,
                          TenantEntitlementRepository entitlementRepository,
                          TenantRepository tenantRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentProvider paymentProvider,
                          AuditService auditService) {
        this.planPriceRepository = planPriceRepository;
        this.addonPriceRepository = addonPriceRepository;
        this.usageService = usageService;
        this.entitlementRepository = entitlementRepository;
        this.tenantRepository = tenantRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentProvider = paymentProvider;
        this.auditService = auditService;
    }

    /** 특정 월의 라이브 청구 명세를 계산(발행 전 미리보기 겸용). */
    @Transactional(readOnly = true)
    public BillingStatement statement(UUID tenantId, String period) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("기관을 찾을 수 없습니다: " + tenantId));
        Plan plan = tenant.getPlan();
        List<InvoiceLine> lines = new ArrayList<>();

        // 1) 요금제 월정액
        int planPrice = planPriceRepository.findById(plan).map(PlanPrice::getMonthlyPrice).orElse(0);
        lines.add(new InvoiceLine("PLAN", plan.getDisplayName() + " 요금제", planPrice, null));

        List<TenantEntitlement> entitlements = entitlementRepository.findByTenantId(tenantId);

        // 2) 정액 애드온 (ADDON으로 부여 + FLAT 가격)
        for (TenantEntitlement e : entitlements) {
            if (e.getSource() != EntitlementSource.ADDON) {
                continue;
            }
            addonPriceRepository.findById(e.getFeature())
                    .filter(p -> p.getPricingType() == PricingType.FLAT && p.getMonthlyPrice() > 0)
                    .ifPresent(p -> lines.add(new InvoiceLine(
                            "ADDON", e.getFeature().getDisplayName() + " (애드온)", p.getMonthlyPrice(), null)));
        }

        // 3) 사용량 과금 (자격 있는 USAGE 가격 기능)
        for (TenantEntitlement e : entitlements) {
            addonPriceRepository.findById(e.getFeature())
                    .filter(p -> p.getPricingType() == PricingType.USAGE)
                    .ifPresent(p -> {
                        Feature f = e.getFeature();
                        int used = usageService.usage(tenantId, f, period);
                        int billable = Math.max(0, used - p.getIncludedUnits());
                        int amount = billable * p.getUnitPrice();
                        String unit = p.getUnitLabel() == null ? "" : p.getUnitLabel();
                        String detail = String.format("%d%s 사용 · %d 무료 포함 · 과금 %d × %,d원",
                                used, unit, p.getIncludedUnits(), billable, p.getUnitPrice());
                        lines.add(new InvoiceLine("USAGE", f.getDisplayName() + " 사용량", amount, detail));
                    });
        }

        // 4) 부가세(소계의 10%)
        int subtotal = lines.stream().mapToInt(InvoiceLine::amount).sum();
        int vat = (int) Math.round(subtotal * VAT_RATE);
        lines.add(new InvoiceLine("TAX", "부가세 (10%)", vat, null));

        int total = subtotal + vat;
        return new BillingStatement(tenantId, period, CURRENCY, total, lines);
    }

    /** 이번 달 명세. */
    @Transactional(readOnly = true)
    public BillingStatement currentStatement(UUID tenantId) {
        return statement(tenantId, BillingPeriods.current());
    }

    /**
     * 해당 월의 인보이스를 발행(현재 명세를 스냅샷). 같은 월 인보이스가 이미 있으면:
     * PAID면 충돌(재발행 불가), ISSUED면 최신 명세로 교체.
     */
    public Invoice issueInvoice(UUID tenantId, String period) {
        BillingStatement s = statement(tenantId, period);
        invoiceRepository.findByTenantIdAndPeriod(tenantId, period).ifPresent(existing -> {
            if ("PAID".equals(existing.getStatus())) {
                throw new ConflictException("이미 결제된 청구서가 있습니다: " + period);
            }
            invoiceRepository.delete(existing);
            invoiceRepository.flush();
        });
        Invoice saved = invoiceRepository.save(new Invoice(tenantId, period, s.currency(), s.total(), s.lines()));
        auditService.record("INVOICE_ISSUE", "INVOICE", saved.getId().toString(),
                String.format("%s 인보이스 발행 · %,d원", period, s.total()));
        return saved;
    }

    /** 인보이스 결제(멱등). 결제 프로바이더가 참조를 반환하면 PAID로 전환. */
    public Invoice payInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("인보이스를 찾을 수 없습니다: " + invoiceId));
        if ("PAID".equals(invoice.getStatus())) {
            return invoice;
        }
        String ref = paymentProvider.charge(invoice);
        invoice.markPaid(ref);
        Invoice saved = invoiceRepository.save(invoice);
        auditService.record("INVOICE_PAY", "INVOICE", invoiceId.toString(),
                String.format("%s 결제 완료 · %,d원 · %s", invoice.getPeriod(), invoice.getTotal(), ref));
        refreshStanding(invoice.getTenantId());   // 연체 해소되면 PAST_DUE → ACTIVE
        return saved;
    }

    /**
     * 청구 마감(자동/수동): 대상 월의 인보이스를 모든 기관에 발행하고 연체 상태를 갱신한다.
     * 이미 결제된 기관은 건너뛴다. 스케줄러가 매월 초 지난달을 마감할 때 호출한다.
     */
    public List<Invoice> closePeriod(String period) {
        List<Invoice> issued = new ArrayList<>();
        for (Tenant t : tenantRepository.findAll()) {
            boolean alreadyPaid = invoiceRepository.findByTenantIdAndPeriod(t.getId(), period)
                    .map(inv -> "PAID".equals(inv.getStatus())).orElse(false);
            if (!alreadyPaid) {
                issued.add(issueInvoice(t.getId(), period));
            }
            refreshStanding(t.getId());
        }
        auditService.record("BILLING_CLOSE", "PERIOD", period,
                String.format("%s 청구 마감 · 발행 %d건", period, issued.size()));
        return issued;
    }

    /**
     * 기관 연체 상태 재계산. 지난 달(들) 미결제 인보이스가 있으면 PAST_DUE, 없으면 ACTIVE.
     * 수동 SUSPENDED는 결제로 자동 해제하지 않는다(그대로 둔다).
     */
    public void refreshStanding(UUID tenantId) {
        Tenant t = tenantRepository.findById(tenantId).orElse(null);
        if (t == null || t.getStatus() == TenantStatus.SUSPENDED) {
            return;
        }
        boolean delinquent = hasUnpaidPastInvoice(tenantId);
        TenantStatus next = delinquent ? TenantStatus.PAST_DUE : TenantStatus.ACTIVE;
        if (t.getStatus() != next) {
            t.setStatus(next);
            tenantRepository.save(t);
        }
    }

    /** 이번 달 이전(period < 현재)의 미결제(ISSUED) 인보이스가 있는가. */
    @Transactional(readOnly = true)
    public boolean hasUnpaidPastInvoice(UUID tenantId) {
        String current = BillingPeriods.current();
        return invoiceRepository.findByTenantIdAndStatus(tenantId, "ISSUED").stream()
                .anyMatch(inv -> inv.getPeriod().compareTo(current) < 0);
    }

    // --- 가격 편집 (슈퍼관리자) ---

    /** 요금제 월정액 수정. */
    public PlanPrice updatePlanPrice(Plan plan, int monthlyPrice) {
        if (monthlyPrice < 0) {
            throw new com.lms.error.BadRequestException("가격은 0 이상이어야 합니다");
        }
        PlanPrice price = planPriceRepository.findById(plan)
                .orElseThrow(() -> new NotFoundException("요금제 가격이 없습니다: " + plan));
        price.setMonthlyPrice(monthlyPrice);
        PlanPrice saved = planPriceRepository.save(price);
        auditService.record("PRICE_UPDATE", "PLAN_PRICE", plan.name(),
                String.format("%s 월정액 → %,d원", plan.name(), monthlyPrice));
        return saved;
    }

    /** 애드온 가격 수정(FLAT/USAGE 필드). */
    public FeatureAddonPrice updateAddonPrice(Feature feature, int monthlyPrice, int unitPrice, int includedUnits) {
        if (monthlyPrice < 0 || unitPrice < 0 || includedUnits < 0) {
            throw new com.lms.error.BadRequestException("가격/수량은 0 이상이어야 합니다");
        }
        FeatureAddonPrice price = addonPriceRepository.findById(feature)
                .orElseThrow(() -> new NotFoundException("애드온 가격이 없습니다: " + feature));
        price.updatePricing(monthlyPrice, unitPrice, includedUnits);
        FeatureAddonPrice saved = addonPriceRepository.save(price);
        auditService.record("PRICE_UPDATE", "ADDON_PRICE", feature.name(),
                String.format("%s 가격 수정 · 정액 %,d / 단가 %,d / 무료 %d", feature.name(), monthlyPrice, unitPrice, includedUnits));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Invoice> invoices(UUID tenantId) {
        return invoiceRepository.findByTenantIdOrderByIssuedAtDesc(tenantId);
    }

    // --- 가격 카탈로그 조회 ---

    @Transactional(readOnly = true)
    public List<PlanPrice> planPrices() {
        return planPriceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FeatureAddonPrice> addonPrices() {
        return addonPriceRepository.findAll();
    }
}
