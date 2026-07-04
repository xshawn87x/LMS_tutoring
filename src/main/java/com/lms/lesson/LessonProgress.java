package com.lms.lesson;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 학습자별 레슨 진도: 마지막 재생 위치(이어듣기)와 완료 여부.
 * tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리한다.
 */
@Entity
@Table(name = "lesson_progress")
@Getter
@NoArgsConstructor
public class LessonProgress extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private String studentId;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private UUID lessonId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "last_position_seconds", nullable = false)
    private int lastPositionSeconds = 0;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public LessonProgress(String studentId, UUID lessonId, UUID courseId) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.courseId = courseId;
    }

    /** 재생 위치와 완료 여부를 갱신한다. 한 번 완료된 레슨은 다시 미완료로 내려가지 않는다. */
    public void apply(int positionSeconds, boolean completedNow) {
        this.lastPositionSeconds = Math.max(0, positionSeconds);
        this.completed = this.completed || completedNow;
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
