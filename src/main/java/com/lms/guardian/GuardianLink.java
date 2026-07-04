package com.lms.guardian;

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

/** 학부모(parentSubject)-자녀(studentSubject) 연결. RLS로 테넌트 격리. */
@Entity
@Table(name = "guardian_link")
@Getter
@NoArgsConstructor
public class GuardianLink extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_subject", nullable = false)
    private String parentSubject;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public GuardianLink(String parentSubject, String studentSubject) {
        this.parentSubject = parentSubject;
        this.studentSubject = studentSubject;
        this.createdAt = OffsetDateTime.now();
    }
}
