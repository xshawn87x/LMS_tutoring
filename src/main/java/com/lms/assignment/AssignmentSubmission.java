package com.lms.assignment;

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

/** 과제 제출 (학생당 과제당 1건, 재제출은 갱신). 채점 시 score/feedback/gradedAt 채움. */
@Entity
@Table(name = "assignment_submission")
@Getter
@NoArgsConstructor
public class AssignmentSubmission extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(nullable = false)
    private String student;

    @Column(name = "text_answer")
    private String textAnswer;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column
    private Integer score;

    @Column
    private String feedback;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;

    public AssignmentSubmission(UUID assignmentId, String student, String textAnswer, String fileUrl) {
        this.assignmentId = assignmentId;
        this.student = student;
        this.textAnswer = textAnswer;
        this.fileUrl = fileUrl;
        this.submittedAt = OffsetDateTime.now();
    }

    /** 재제출: 답안 갱신 + 채점 초기화. */
    public void resubmit(String textAnswer, String fileUrl) {
        this.textAnswer = textAnswer;
        this.fileUrl = fileUrl;
        this.submittedAt = OffsetDateTime.now();
        this.score = null;
        this.feedback = null;
        this.gradedAt = null;
    }

    public void grade(int score, String feedback) {
        this.score = score;
        this.feedback = feedback;
        this.gradedAt = OffsetDateTime.now();
    }
}
