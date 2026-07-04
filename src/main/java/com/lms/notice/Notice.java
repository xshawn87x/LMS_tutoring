package com.lms.notice;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 공지. scope=ACADEMY(학원 전체) 또는 COURSE(특정 강의, courseId 지정). */
@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor
public class Notice extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeScope scope;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column
    private String body;

    @Column
    private String author;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Notice(NoticeScope scope, UUID courseId, String title, String body, String author, boolean pinned) {
        this.scope = scope;
        this.courseId = courseId;
        this.title = title;
        this.body = body;
        this.author = author;
        this.pinned = pinned;
        this.createdAt = OffsetDateTime.now();
    }

    public void update(String title, String body, boolean pinned) {
        this.title = title;
        this.body = body;
        this.pinned = pinned;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
