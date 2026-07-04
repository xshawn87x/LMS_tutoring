package com.lms.billing;

import com.lms.feature.Feature;
import com.lms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용량 과금 기능(AI 분석 등)의 월별 사용량을 계량한다.
 * {@code usage_counter}는 RLS-free 전역이므로 tenant_id로 명시 스코프한다.
 */
@Service
@Transactional
public class UsageService {

    private final UsageCounterRepository repository;

    public UsageService(UsageCounterRepository repository) {
        this.repository = repository;
    }

    /** 현재 요청 테넌트의 이번 달 사용량을 1 증가. 테넌트 컨텍스트가 없으면 무시(계량 대상 아님). */
    public void record(Feature feature) {
        UUID tenant = TenantContext.get().orElse(null);
        if (tenant == null) {
            return;
        }
        String period = BillingPeriods.current();
        UsageCounter counter = repository.findByTenantIdAndFeatureAndPeriod(tenant, feature, period)
                .orElseGet(() -> new UsageCounter(tenant, feature, period));
        counter.increment(1);
        repository.save(counter);
    }

    /** 특정 테넌트·기능·주기의 누적 사용량. */
    @Transactional(readOnly = true)
    public int usage(UUID tenantId, Feature feature, String period) {
        return repository.findByTenantIdAndFeatureAndPeriod(tenantId, feature, period)
                .map(UsageCounter::getCount)
                .orElse(0);
    }
}
