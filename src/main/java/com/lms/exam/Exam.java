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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 시험 정의 (자체 시험/모의고사). 과목·시행일·만점, 선택적으로 반에 연결. */
@Entity
@Table(name = "exam")
@Getter
@NoArgsConstructor
public class Exam extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column
    private String subject;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Exam(String title, String subject, LocalDate examDate, int maxScore, UUID groupId) {
        this.title = title;
        this.subject = subject;
        this.examDate = examDate;
        this.maxScore = maxScore;
        this.groupId = groupId;
    }

    public void update(String title, String subject, LocalDate examDate, int maxScore, UUID groupId) {
        this.title = title;
        this.subject = subject;
        this.examDate = examDate;
        this.maxScore = maxScore;
        this.groupId = groupId;
    }
}
