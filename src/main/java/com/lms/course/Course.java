package com.lms.course;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 과정(Course). 행 격리는 RLS가 강제하고, tenant_id는 {@link TenantOwned}가 쓰기 시 자동으로 채운다.
 * 따라서 도메인/서비스 코드는 테넌트를 의식하지 않는다.
 */
@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor
public class Course extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    /** 관심분야 코드 (interest_category.code). 강사가 지정; 추천 매칭에 사용. */
    @Column(name = "category_code")
    private String categoryCode;

    /** 난이도 0(입문)~3(고급). */
    @Column
    private Integer level;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 노출 여부. false면 학생 카탈로그에서 숨김(강사/관리자에겐 보임). */
    @Column(nullable = false)
    private boolean published = true;

    /** 수강료(원). 0이면 무료. */
    @Column(name = "tuition_fee", nullable = false)
    private int tuitionFee = 0;

    public Course(String title, String description, String categoryCode, Integer level) {
        this.title = title;
        this.description = description;
        this.categoryCode = categoryCode;
        this.level = level;
        this.published = true;
    }

    /** 과정 정보 수정 (강사/관리자). */
    public void update(String title, String description, String categoryCode, Integer level) {
        this.title = title;
        this.description = description;
        this.categoryCode = categoryCode;
        this.level = level;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setTuitionFee(int tuitionFee) {
        this.tuitionFee = Math.max(0, tuitionFee);
    }
}
