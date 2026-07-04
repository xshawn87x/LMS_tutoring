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

/** 퀴즈 제출/채점 결과: 학생당 점수 기록. tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리. */
@Entity
@Table(name = "quiz_submission")
@Getter
@NoArgsConstructor
public class QuizSubmission extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false, updatable = false)
    private UUID quizId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private OffsetDateTime submittedAt;

    public QuizSubmission(UUID quizId, String studentId, int score, int total) {
        this.quizId = quizId;
        this.studentId = studentId;
        this.score = score;
        this.total = total;
    }
}
