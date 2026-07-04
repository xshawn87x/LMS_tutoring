package com.lms.learner;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** 학습자의 관심분야 선택 (student_id × category_code). */
@Entity
@Table(name = "learner_interest")
@Getter
@NoArgsConstructor
public class LearnerInterest extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(name = "category_code", nullable = false, updatable = false)
    private String categoryCode;

    public LearnerInterest(String studentId, String categoryCode) {
        this.studentId = studentId;
        this.categoryCode = categoryCode;
    }
}
