package com.lms.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantFeatureRepository extends JpaRepository<TenantFeature, UUID> {
    // RLS가 현재 테넌트로 격리하므로 feature만으로 조회한다.
    Optional<TenantFeature> findByFeature(Feature feature);
}
