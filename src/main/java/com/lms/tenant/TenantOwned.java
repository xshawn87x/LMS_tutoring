package com.lms.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.util.UUID;

/**
 * 테넌트에 귀속되는 엔티티의 공통 상위 타입.
 *
 * tenant_id 컬럼과, 저장 시 현재 테넌트(TenantContext)로 자동 채우는 @PrePersist 콜백을 한곳에 둔다.
 * 모든 도메인 엔티티(Course, Lesson, Enrollment, Quiz, Question, QuizSubmission)가 이를 상속한다.
 * 행 격리는 여전히 DB(RLS)가 강제하며, 이 클래스는 "쓰기 시 올바른 tenant_id를 박는" 책임만 진다.
 */
@MappedSuperclass
public abstract class TenantOwned {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    public UUID getTenantId() {
        return tenantId;
    }

    @PrePersist
    void assignTenant() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.get()
                    .orElseThrow(() -> new IllegalStateException("테넌트 컨텍스트 없이 엔티티를 생성할 수 없습니다"));
        }
    }
}
