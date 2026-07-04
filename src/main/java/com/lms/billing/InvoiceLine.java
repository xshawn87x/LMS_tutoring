package com.lms.billing;

/**
 * 인보이스 라인아이템(발행 시점 스냅샷).
 * kind = PLAN | ADDON | USAGE, amount = KRW, detail = 산출 근거(예: "18회 × 500원 (50 무료 포함)").
 */
public record InvoiceLine(
        String kind,
        String label,
        int amount,
        String detail) {
}
