package com.lms.enrollment;

import com.lms.enrollment.dto.EnrollmentResponse;
import com.lms.enrollment.dto.ProgressRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 수강신청 API. 학생 식별자(student_id)는 JWT의 subject에서 가져온다.
 * 토큰의 tenant_id로 테넌트가, subject로 학생이 결정된다.
 */
@RestController
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    /** 현재 사용자가 과정을 수강신청한다. */
    @PostMapping("/api/courses/{courseId}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public EnrollmentResponse enroll(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        return EnrollmentResponse.from(service.enroll(courseId, jwt.getSubject()));
    }

    /** 내 수강 목록. */
    @GetMapping("/api/enrollments/me")
    public List<EnrollmentResponse> myEnrollments(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(jwt.getSubject()).stream().map(EnrollmentResponse::from).toList();
    }

    /** 내 수강의 진도를 갱신한다 (100%면 자동 완료). */
    @PatchMapping("/api/enrollments/{id}/progress")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public EnrollmentResponse updateProgress(@PathVariable UUID id,
                                             @Valid @RequestBody ProgressRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        return EnrollmentResponse.from(service.updateProgress(id, jwt.getSubject(), request.progress()));
    }

    /** 수강 취소 (내 수강 + 진도·수료증 초기화). */
    @DeleteMapping("/api/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.cancel(id, jwt.getSubject());
    }
}
