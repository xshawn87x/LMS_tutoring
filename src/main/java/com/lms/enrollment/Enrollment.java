package com.lms.enrollment;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 수강신청(Enrollment): 학생(student_id = JWT subject)이 과정을 수강.
 * 진도(progress 0~100)와 상태(status)를 추적한다. tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리.
 */
@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor
public class Enrollment extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "enrolled_at", insertable = false, updatable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Enrollment(UUID courseId, String studentId) {
        this.courseId = courseId;
        this.studentId = studentId;
    }

    /** 진도를 갱신하고 100%가 되면 자동으로 완료 처리한다. */
    public void updateProgress(int newProgress) {
        this.progress = newProgress;
        this.status = (newProgress >= 100) ? EnrollmentStatus.COMPLETED : EnrollmentStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
