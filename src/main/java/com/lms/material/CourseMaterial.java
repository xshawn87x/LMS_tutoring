package com.lms.material;

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

/** 강의 자료(교재/첨부). 파일은 업로드 API로 올린 URL을 저장. */
@Entity
@Table(name = "course_material")
@Getter
@NoArgsConstructor
public class CourseMaterial extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public CourseMaterial(UUID courseId, String title, String fileUrl, String uploadedBy) {
        this.courseId = courseId;
        this.title = title;
        this.fileUrl = fileUrl;
        this.uploadedBy = uploadedBy;
        this.createdAt = OffsetDateTime.now();
    }
}
