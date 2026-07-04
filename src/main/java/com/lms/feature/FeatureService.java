package com.lms.feature;

import com.lms.platform.AccountStandingService;
import com.lms.platform.EntitlementService;
import com.lms.platform.FeatureNotEntitledException;
import com.lms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 현재 테넌트의 기능 상태를 읽고/바꾸고/강제한다. 2계층으로 판정한다:
 * <ol>
 *   <li>계층①(자격): {@link EntitlementService} — 요금제로 이 기능을 쓸 수 있는가(플랫폼이 부여).
 *   <li>계층②(활성화): tenant_feature override(없으면 {@link Feature#isDefaultEnabled()}) — 기관이 켰는가.
 * </ol>
 * <b>유효 활성(isEnabled) = 자격 있음 AND 기관이 켬.</b> 자격 없는 기능은 기관 ADMIN이 켤 수 없다.
 */
@Service
@Transactional
public class FeatureService {

    private final TenantFeatureRepository repository;
    private final EntitlementService entitlementService;
    private final AccountStandingService accountStandingService;

    public FeatureService(TenantFeatureRepository repository, EntitlementService entitlementService,
                          AccountStandingService accountStandingService) {
        this.repository = repository;
        this.entitlementService = entitlementService;
        this.accountStandingService = accountStandingService;
    }

    /** 유효 활성 = 자격 있음 AND 기관이 켬. 둘 중 하나라도 아니면 false. */
    @Transactional(readOnly = true)
    public boolean isEnabled(Feature feature) {
        UUID tenant = TenantContext.get().orElse(null);
        if (tenant == null || !entitlementService.isEntitled(tenant, feature)) {
            return false;
        }
        return isActivated(feature);
    }

    /** 계층②만: 기관 ADMIN의 on/off 선택(자격과 무관). override 없으면 enum 기본값. */
    @Transactional(readOnly = true)
    public boolean isActivated(Feature feature) {
        return repository.findByFeature(feature)
                .map(TenantFeature::isEnabled)
                .orElse(feature.isDefaultEnabled());
    }

    @Transactional(readOnly = true)
    public List<FeatureView> list() {
        UUID tenant = TenantContext.get().orElse(null);
        Set<com.lms.feature.Feature> entitled = tenant == null ? Set.of() : entitlementService.entitledFeatures(tenant);
        return Arrays.stream(Feature.values())
                .map(f -> FeatureView.of(f, entitled.contains(f), isActivated(f)))
                .toList();
    }

    /**
     * 기능 활성화 토글 (upsert). ADMIN 전용은 컨트롤러에서 강제.
     * 자격이 없으면 켤 수 없다({@link FeatureNotEntitledException}, 403).
     */
    public FeatureView setEnabled(Feature feature, boolean enabled) {
        UUID tenant = TenantContext.get().orElseThrow(() -> new FeatureNotEntitledException(feature));
        accountStandingService.requireActive(tenant);   // 정지/연체 기관은 설정 변경도 불가
        if (!entitlementService.isEntitled(tenant, feature)) {
            throw new FeatureNotEntitledException(feature);
        }
        TenantFeature row = repository.findByFeature(feature)
                .orElseGet(() -> new TenantFeature(feature, enabled));
        row.setEnabled(enabled);
        repository.save(row);
        return FeatureView.of(feature, true, enabled);
    }

    /**
     * 모듈 이용 게이트. 각 모듈 서비스 진입부에서 호출한다.
     * 순서: ① 기관 이용 상태(정지/연체) → ② 자격 → ③ 활성화. 하나라도 막히면 403.
     */
    @Transactional(readOnly = true)
    public void requireEnabled(Feature feature) {
        TenantContext.get().ifPresent(accountStandingService::requireActive);
        if (!isEnabled(feature)) {
            throw new FeatureDisabledException(feature);
        }
    }
}
