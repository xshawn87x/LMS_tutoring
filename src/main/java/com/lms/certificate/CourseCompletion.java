package com.lms.certificate;

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

/** 수료(과정 완료) 기록 = 수료증. tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리. */
@Entity
@Table(name = "course_completion")
@Getter
@NoArgsConstructor
public class CourseCompletion extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(name = "certificate_no", nullable = false, updatable = false)
    private String certificateNo;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private OffsetDateTime issuedAt;

    public CourseCompletion(UUID courseId, String studentId, String certificateNo) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.certificateNo = certificateNo;
    }
}
