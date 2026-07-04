package com.lms.platform;

import com.lms.feature.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 자격 조회/변경. RLS 없는 전역 테이블이므로 <b>반드시 tenant_id로 명시적으로 스코프</b>한다
 * (RLS에 의존하지 않는다).
 */
public interface TenantEntitlementRepository extends JpaRepository<TenantEntitlement, UUID> {

    List<TenantEntitlement> findByTenantId(UUID tenantId);

    Optional<TenantEntitlement> findByTenantIdAndFeature(UUID tenantId, Feature feature);

    boolean existsByTenantIdAndFeature(UUID tenantId, Feature feature);
}
