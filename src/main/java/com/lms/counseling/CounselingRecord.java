package com.lms.counseling;

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

/** 학생 상담 기록 (강사/관리자 작성). */
@Entity
@Table(name = "counseling_record")
@Getter
@NoArgsConstructor
public class CounselingRecord extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(nullable = false)
    private String counselor;

    @Column(nullable = false)
    private String content;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public CounselingRecord(String studentSubject, String counselor, String content) {
        this.studentSubject = studentSubject;
        this.counselor = counselor;
        this.content = content;
        this.createdAt = OffsetDateTime.now();
    }
}
