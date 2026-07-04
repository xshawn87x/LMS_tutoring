package com.lms.quiz;

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

/** 퀴즈: 과정에 속한 평가. tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리. */
@Entity
@Table(name = "quiz")
@Getter
@NoArgsConstructor
public class Quiz extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Quiz(UUID courseId, String title) {
        this.courseId = courseId;
        this.title = title;
    }
}
