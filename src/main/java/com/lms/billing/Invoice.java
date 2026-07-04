package com.lms.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 특정 월의 청구 스냅샷(요금제+애드온+사용량) + 결제 상태.
 * lines는 발행 시점 가격을 그대로 보존한다(이후 가격 변경과 무관). RLS-free(전역).
 */
@Entity
@Table(name = "invoice")
@Getter
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** 'YYYY-MM' */
    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private String status;   // ISSUED | PAID

    @Convert(converter = InvoiceLinesConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<InvoiceLine> lines;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    public Invoice(UUID tenantId, String period, String currency, int total, List<InvoiceLine> lines) {
        this.tenantId = tenantId;
        this.period = period;
        this.currency = currency;
        this.total = total;
        this.lines = lines;
        this.status = "ISSUED";
        this.issuedAt = OffsetDateTime.now();
    }

    /** 결제 완료 처리(결제 프로바이더가 반환한 참조를 기록). */
    public void markPaid(String paymentRef) {
        this.status = "PAID";
        this.paymentRef = paymentRef;
        this.paidAt = OffsetDateTime.now();
    }
}
