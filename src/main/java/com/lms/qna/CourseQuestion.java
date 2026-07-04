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

/** 강의 질문. 수강생이 작성, 강사/관리자가 답변한다. */
@Entity
@Table(name = "course_question")
@Getter
@NoArgsConstructor
public class CourseQuestion extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String title;

    @Column
    private String body;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public CourseQuestion(UUID courseId, String author, String title, String body) {
        this.courseId = courseId;
        this.author = author;
        this.title = title;
        this.body = body;
        this.resolved = false;
        this.createdAt = OffsetDateTime.now();
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
