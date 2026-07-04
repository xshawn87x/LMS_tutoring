package com.lms.market;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 학원(테넌트)의 콘텐츠 구매 내역. 플랫폼이 정산 위해 테넌트 경계를 넘어 집계하므로
 * RLS 없는 전역 테이블이며 tenant_id를 명시적으로 스코프한다(TenantOwned 미상속).
 */
@Entity
@Table(name = "content_purchase")
@Getter
@NoArgsConstructor
public class ContentPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "purchased_by")
    private String purchasedBy;

    @Column(nullable = false)
    private int amount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public ContentPurchase(UUID tenantId, UUID contentId, String purchasedBy, int amount) {
        this.tenantId = tenantId;
        this.contentId = contentId;
        this.purchasedBy = purchasedBy;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now();
    }
}
