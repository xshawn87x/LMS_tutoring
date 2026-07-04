package com.lms.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // RLS 없는 전역 레지스트리 — org_code로 테넌트 식별.
    Optional<Tenant> findByOrgCode(String orgCode);
}
