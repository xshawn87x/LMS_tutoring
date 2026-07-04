package com.lms.material;

import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 자료실 API. 조회는 테넌트 내 누구나(다운로드), 업로드/삭제는 INSTRUCTOR/ADMIN. RLS 격리. */
@RestController
@Transactional
public class MaterialController {

    private final CourseMaterialRepository repository;
    private final CourseService courseService;

    public MaterialController(CourseMaterialRepository repository, CourseService courseService) {
        this.repository = repository;
        this.courseService = courseService;
    }

    @GetMapping("/api/courses/{courseId}/materials")
    @Transactional(readOnly = true)
    public List<MaterialResponse> list(@PathVariable UUID courseId) {
        courseService.requireExists(courseId);
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(MaterialResponse::from).toList();
    }

    @PostMapping("/api/courses/{courseId}/materials")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public MaterialResponse create(@PathVariable UUID courseId, @RequestBody MaterialRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        courseService.requireExists(courseId);
        CourseMaterial m = repository.save(
                new CourseMaterial(courseId, request.title(), request.fileUrl(), jwt.getSubject()));
        return MaterialResponse.from(m);
    }

    @DeleteMapping("/api/materials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        CourseMaterial m = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("자료를 찾을 수 없습니다: " + id));
        repository.delete(m);
    }

    public record MaterialRequest(@NotBlank String title, @NotBlank String fileUrl) {
    }

    public record MaterialResponse(UUID id, UUID courseId, String title, String fileUrl,
                                   String uploadedBy, OffsetDateTime createdAt) {
        static MaterialResponse from(CourseMaterial m) {
            return new MaterialResponse(m.getId(), m.getCourseId(), m.getTitle(), m.getFileUrl(),
                    m.getUploadedBy(), m.getCreatedAt());
        }
    }
}
