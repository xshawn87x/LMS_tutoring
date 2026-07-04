package com.lms.exam;

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

/** 한 시험에 대한 학생별 점수 (시험+학생 단위, 유니크). */
@Entity
@Table(name = "exam_score")
@Getter
@NoArgsConstructor
public class ExamScore extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_subject", nullable = false)
    private String studentSubject;

    @Column(nullable = false)
    private int score;

    @Column
    private String comment;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ExamScore(UUID examId, String studentSubject, int score, String comment) {
        this.examId = examId;
        this.studentSubject = studentSubject;
        this.score = score;
        this.comment = comment;
    }

    public void update(int score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}
