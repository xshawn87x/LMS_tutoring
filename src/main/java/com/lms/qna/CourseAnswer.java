package com.lms.qna;

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

/** 질문에 대한 답변. */
@Entity
@Table(name = "course_answer")
@Getter
@NoArgsConstructor
public class CourseAnswer extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public CourseAnswer(UUID questionId, String author, String body) {
        this.questionId = questionId;
        this.author = author;
        this.body = body;
        this.createdAt = OffsetDateTime.now();
    }
}
