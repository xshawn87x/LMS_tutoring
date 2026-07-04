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

/** 학습자의 분야별 자가 진단 수준 (0 입문 ~ 3 고급). */
@Entity
@Table(name = "learner_skill")
@Getter
@NoArgsConstructor
public class LearnerSkill extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(name = "category_code", nullable = false, updatable = false)
    private String categoryCode;

    @Column(nullable = false)
    private int level;

    public LearnerSkill(String studentId, String categoryCode, int level) {
        this.studentId = studentId;
        this.categoryCode = categoryCode;
        this.level = level;
    }
}
