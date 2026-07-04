package com.lms.learner;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
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
import java.util.UUID;

/** 학습자 1명(테넌트 내 student_id = JWT subject)의 프로필. tenant_id는 {@link TenantOwned}가 채운다. */
@Entity
@Table(name = "learner_profile")
@Getter
@NoArgsConstructor
public class LearnerProfile extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(nullable = false)
    private boolean onboarded;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public LearnerProfile(String studentId) {
        this.studentId = studentId;
    }

    public void markOnboarded() {
        this.onboarded = true;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
