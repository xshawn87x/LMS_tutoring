package com.lms.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** RLS-free 전역 → 반드시 tenant_id로 스코프. */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByTenantIdOrderByIssuedAtDesc(UUID tenantId);

    Optional<Invoice> findByTenantIdAndPeriod(UUID tenantId, String period);

    List<Invoice> findByTenantIdAndStatus(UUID tenantId, String status);
}
