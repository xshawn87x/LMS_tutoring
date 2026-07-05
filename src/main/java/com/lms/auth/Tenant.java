package com.lms.auth;

import com.lms.platform.Plan;
import com.lms.platform.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 기관(Tenant) 레지스트리. 로그인/회원가입 시 org_code로 테넌트를 찾는다.
 * 테넌트 미상 단계에서 조회해야 하므로 RLS 대상이 아닌 전역 테이블이다.
 *
 * <p>plan: SaaS 요금제 — 이 기관이 자격을 가진 기능 묶음을 결정한다(플랫폼이 부여).
 */
@Entity
@Table(name = "tenant")
@Getter
@NoArgsConstructor
public class Tenant {

    @Id
    private UUID id;

    @Column(name = "org_code", nullable = false, unique = true)
    private String orgCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private TenantStatus status;

    public Tenant(UUID id, String orgCode, String name, Plan plan, TenantStatus status) {
        this.id = id;
        this.orgCode = orgCode;
        this.name = name;
        this.plan = plan;
        this.status = status;
    }
}
