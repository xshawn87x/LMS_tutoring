package com.lms.curation;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 과정 콘텐츠 분석 결과(큐레이션). tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리. */
@Entity
@Table(name = "content_insight")
@Getter
@NoArgsConstructor
public class ContentInsight extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Convert(converter = TagsJsonConverter.class)
    @Column(nullable = false)
    private List<String> tags;

    @Column
    private Integer difficulty;

    @Column
    private String summary;

    @Column(name = "est_minutes")
    private Integer estMinutes;

    @Column(name = "generated_by", nullable = false)
    private String generatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ContentInsight(UUID courseId) {
        this.courseId = courseId;
    }

    /** 분석 결과로 갱신(재분석 시 덮어쓰기). */
    public void apply(List<String> tags, Integer difficulty, String summary, Integer estMinutes, String generatedBy) {
        this.tags = tags;
        this.difficulty = difficulty;
        this.summary = summary;
        this.estMinutes = estMinutes;
        this.generatedBy = generatedBy;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
