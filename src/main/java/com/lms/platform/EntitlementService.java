package com.lms.platform;

import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 계층①(자격) 관리: 어떤 기관이 어떤 기능을 "쓸 수 있는가".
 *
 * <p>플랫폼(슈퍼관리자)이 요금제/애드온으로 부여한다. {@code tenant_entitlement}는 RLS 없는 전역
 * 테이블이므로 여기서는 항상 tenant_id로 명시적으로 스코프한다.
 */
@Service
@Transactional
public class EntitlementService {

    private final TenantEntitlementRepository repository;
    private final TenantRepository tenantRepository;

    public EntitlementService(TenantEntitlementRepository repository, TenantRepository tenantRepository) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
    }

    /** 이 기관이 이 기능을 쓸 자격이 있는가. */
    @Transactional(readOnly = true)
    public boolean isEntitled(UUID tenantId, Feature feature) {
        return repository.existsByTenantIdAndFeature(tenantId, feature);
    }

    /** 이 기관이 자격을 가진 기능 집합. */
    @Transactional(readOnly = true)
    public Set<Feature> entitledFeatures(UUID tenantId) {
        Set<Feature> result = EnumSet.noneOf(Feature.class);
        repository.findByTenantId(tenantId).forEach(e -> result.add(e.getFeature()));
        return result;
    }

    /**
     * 기관의 요금제를 바꾼다. 새 요금제가 포함하는 기능은 모두 PLAN 자격이 되고(기존에 ADDON이었어도
     * 요금제 안으로 "흡수"), 요금제 밖의 ADDON은 그대로 유지된다. (플랜 + 애드온 하이브리드)
     */
    public void applyPlan(UUID tenantId, Plan plan) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("기관을 찾을 수 없습니다: " + tenantId));
        tenant.setPlan(plan);
        tenantRepository.save(tenant);

        // 기존 PLAN 행 + "새 요금제가 포함하는" 기능 행(ADDON 포함)을 제거한다.
        // → 요금제가 커버하는 기능은 PLAN으로 재부여되어 source가 정규화되고, 요금제 밖 ADDON만 남는다.
        // 남은 행과 유니크(tenant,feature)가 충돌하지 않도록 삽입 전에 delete를 먼저 flush.
        repository.findByTenantId(tenantId).stream()
                .filter(e -> e.getSource() == EntitlementSource.PLAN || plan.features().contains(e.getFeature()))
                .forEach(repository::delete);
        repository.flush();

        for (Feature feature : plan.features()) {
            repository.save(new TenantEntitlement(tenantId, feature, EntitlementSource.PLAN));
        }
    }

    /** 요금제 밖에서 개별 기능 자격을 추가로 부여(애드온). 이미 있으면 무시. */
    public void grantAddon(UUID tenantId, Feature feature) {
        if (!repository.existsByTenantIdAndFeature(tenantId, feature)) {
            repository.save(new TenantEntitlement(tenantId, feature, EntitlementSource.ADDON));
        }
    }

    /** 자격 회수. PLAN이든 ADDON이든 해당 (tenant, feature) 행을 제거한다. */
    public void revokeEntitlement(UUID tenantId, Feature feature) {
        repository.findByTenantIdAndFeature(tenantId, feature).ifPresent(repository::delete);
    }
}
