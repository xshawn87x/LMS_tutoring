package com.lms.market;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** RLS 없는 전역 → 반드시 tenant_id로 스코프(정산은 전체 조회). */
public interface ContentPurchaseRepository extends JpaRepository<ContentPurchase, UUID> {
    List<ContentPurchase> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndContentId(UUID tenantId, UUID contentId);
}
