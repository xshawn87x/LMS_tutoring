package com.lms.platform;

import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.error.NotFoundException;
import com.lms.platform.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 기관 이용 상태(ACTIVE/PAST_DUE/SUSPENDED) 판정·강제.
 * FeatureService가 모듈 이용 전에 {@link #requireActive}로 게이팅한다.
 *
 * <p>상태 전환 중 자동(연체/결제)은 BillingService가, 수동(정지/해제)은 여기서 처리한다.
 * (delinquency 계산은 인보이스에 의존하므로 billing 쪽에 두어 순환 의존을 피한다.)
 */
@Service
@Transactional
public class AccountStandingService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public AccountStandingService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    /** 이용 상태가 ACTIVE가 아니면 차단(403). 기관을 못 찾으면 통과(방어적). */
    @Transactional(readOnly = true)
    public void requireActive(UUID tenantId) {
        tenantRepository.findById(tenantId).ifPresent(t -> {
            if (t.getStatus() != TenantStatus.ACTIVE) {
                throw new AccountSuspendedException(t.getStatus());
            }
        });
    }

    /** 수동 정지(슈퍼관리자). */
    public void suspend(UUID tenantId) {
        Tenant t = require(tenantId);
        t.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(t);
        auditService.record("TENANT_SUSPEND", "TENANT", tenantId.toString(), "기관 수동 정지");
    }

    /** 수동 해제(슈퍼관리자) → ACTIVE. */
    public void reactivate(UUID tenantId) {
        Tenant t = require(tenantId);
        t.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(t);
        auditService.record("TENANT_REACTIVATE", "TENANT", tenantId.toString(), "기관 이용 재개");
    }

    private Tenant require(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("기관을 찾을 수 없습니다: " + tenantId));
    }
}
