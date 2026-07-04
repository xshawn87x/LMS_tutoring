package com.lms.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** RLS로 현재 테넌트의 행만 보인다. */
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {
}
