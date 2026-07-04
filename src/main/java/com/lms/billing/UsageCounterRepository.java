package com.lms.billing;

import com.lms.feature.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** RLS-free 전역 → 반드시 tenant_id로 스코프. */
public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {

    Optional<UsageCounter> findByTenantIdAndFeatureAndPeriod(UUID tenantId, Feature feature, String period);

    List<UsageCounter> findByTenantId(UUID tenantId);
}
