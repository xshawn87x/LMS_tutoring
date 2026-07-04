package com.lms.billing;

import java.util.List;
import java.util.UUID;

/** 특정 월의 청구 명세(라이브 계산 결과). 인보이스로 발행하면 이 스냅샷이 저장된다. */
public record BillingStatement(
        UUID tenantId,
        String period,
        String currency,
        int total,
        List<InvoiceLine> lines) {
}
