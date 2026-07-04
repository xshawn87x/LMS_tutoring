package com.lms.billing.dto;

import com.lms.billing.BillingStatement;
import com.lms.billing.FeatureAddonPrice;
import com.lms.billing.Invoice;
import com.lms.billing.InvoiceLine;
import com.lms.billing.PlanPrice;
import com.lms.platform.Plan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 플랫폼 결제/가격 API의 응답 DTO. */
public final class BillingDtos {

    private BillingDtos() {
    }

    public record PlanPriceView(String name, String displayName, int monthlyPrice, String currency) {
        public static PlanPriceView from(PlanPrice p) {
            return new PlanPriceView(p.getPlan().name(), p.getPlan().getDisplayName(), p.getMonthlyPrice(), p.getCurrency());
        }
    }

    public record AddonPriceView(
            String feature, String displayName, String pricingType,
            int monthlyPrice, int unitPrice, int includedUnits, String unitLabel, String currency) {
        public static AddonPriceView from(FeatureAddonPrice p) {
            return new AddonPriceView(
                    p.getFeature().name(), p.getFeature().getDisplayName(), p.getPricingType().name(),
                    p.getMonthlyPrice(), p.getUnitPrice(), p.getIncludedUnits(), p.getUnitLabel(), p.getCurrency());
        }
    }

    /** 가격 카탈로그. */
    public record PricingView(List<PlanPriceView> plans, List<AddonPriceView> addons) {
    }

    public record InvoiceLineView(String kind, String label, int amount, String detail) {
        public static InvoiceLineView from(InvoiceLine l) {
            return new InvoiceLineView(l.kind(), l.label(), l.amount(), l.detail());
        }
    }

    /** 라이브 청구 명세(발행 전 미리보기). */
    public record StatementView(String period, String currency, int total, List<InvoiceLineView> lines) {
        public static StatementView from(BillingStatement s) {
            return new StatementView(s.period(), s.currency(), s.total(),
                    s.lines().stream().map(InvoiceLineView::from).toList());
        }
    }

    public record InvoiceView(
            UUID id, UUID tenantId, String period, String currency, int total, String status,
            List<InvoiceLineView> lines, String paymentRef, OffsetDateTime issuedAt, OffsetDateTime paidAt) {
        public static InvoiceView from(Invoice i) {
            return new InvoiceView(
                    i.getId(), i.getTenantId(), i.getPeriod(), i.getCurrency(), i.getTotal(), i.getStatus(),
                    i.getLines().stream().map(InvoiceLineView::from).toList(),
                    i.getPaymentRef(), i.getIssuedAt(), i.getPaidAt());
        }
    }

    /** 한 기관의 현재 명세 + 인보이스 이력. */
    public record TenantBillingView(StatementView statement, List<InvoiceView> invoices) {
    }

    public record IssueInvoiceRequest(String period) {
    }

    /** 요금제 월정액 수정 요청. */
    public record PlanPriceUpdateRequest(int monthlyPrice) {
    }

    /** 애드온 가격 수정 요청(FLAT은 monthlyPrice, USAGE는 unitPrice/includedUnits 사용). */
    public record AddonPriceUpdateRequest(int monthlyPrice, int unitPrice, int includedUnits) {
    }

    // Plan 표시명 접근용(컨트롤러 편의)
    public static String planDisplayName(String plan) {
        try {
            return Plan.valueOf(plan).getDisplayName();
        } catch (IllegalArgumentException e) {
            return plan;
        }
    }
}
