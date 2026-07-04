package com.lms.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    // tenant_id 필터는 없다 — RLS가 현재 테넌트로 자동 격리.
    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
