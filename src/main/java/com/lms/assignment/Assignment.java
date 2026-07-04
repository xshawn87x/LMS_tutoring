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

/** 강의 과제. */
@Entity
@Table(name = "assignment")
@Getter
@NoArgsConstructor
public class Assignment extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Assignment(UUID courseId, String title, String description, OffsetDateTime dueAt, int maxScore) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.maxScore = maxScore <= 0 ? 100 : maxScore;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(String title, String description, OffsetDateTime dueAt, int maxScore) {
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.maxScore = maxScore <= 0 ? 100 : maxScore;
    }
}
