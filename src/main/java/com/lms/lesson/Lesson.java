package com.lms.lesson;

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

/**
 * 레슨(Lesson): 과정(Course)에 속한 학습 콘텐츠. tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리한다.
 */
@Entity
@Table(name = "lesson")
@Getter
@NoArgsConstructor
public class Lesson extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column
    private String content;

    /** 동영상 URL. 없으면 프론트가 샘플 영상으로 대체. */
    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Lesson(UUID courseId, String title, String content, String videoUrl, int orderNo) {
        this.courseId = courseId;
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.orderNo = orderNo;
    }

    /** 레슨 내용/순서 수정 (강사/관리자). courseId는 바뀌지 않는다. */
    public void update(String title, String content, String videoUrl, int orderNo) {
        this.title = title;
        this.content = content;
        this.videoUrl = videoUrl;
        this.orderNo = orderNo;
    }
}
