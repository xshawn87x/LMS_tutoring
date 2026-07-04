package com.lms.platform;

import com.lms.feature.Feature;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 한 기관(테넌트)이 한 기능을 "쓸 수 있음"을 나타내는 자격 행.
 *
 * <p>플랫폼(슈퍼관리자)이 테넌트 경계를 넘어 관리하므로 <b>RLS 대상이 아니다</b>
 * (tenant 레지스트리와 동일). 따라서 {@code TenantOwned}를 상속하지 않고 tenant_id를 명시적으로 갖는다.
 * 앱은 항상 tenant_id로 스코프해서 조회한다.
 */
@Entity
@Table(name = "tenant_entitlement")
@Getter
@NoArgsConstructor
public class TenantEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Feature feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntitlementSource source;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public TenantEntitlement(UUID tenantId, Feature feature, EntitlementSource source) {
        this.tenantId = tenantId;
        this.feature = feature;
        this.source = source;
        this.createdAt = OffsetDateTime.now();
    }
}
