package com.lms.lesson;

import com.lms.lesson.dto.LessonProgressRequest;
import com.lms.lesson.dto.LessonProgressResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 학습창 진도 API. 학생 식별자(student_id)는 JWT subject에서 가져온다.
 * 재생 위치 저장(이어듣기) + 완료 처리 → 수강 진도 자동 재계산.
 */
@RestController
public class LessonProgressController {

    private final LessonProgressService service;

    public LessonProgressController(LessonProgressService service) {
        this.service = service;
    }

    /** 레슨 진도 저장(이어듣기 위치 + 완료 여부). */
    @PutMapping("/api/lessons/{lessonId}/progress")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public LessonProgressResponse save(@PathVariable UUID lessonId,
                                       @Valid @RequestBody LessonProgressRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        return LessonProgressResponse.from(service.save(lessonId, jwt.getSubject(), request));
    }

    /** 과정의 내 레슨 진도 목록(학습창 진입 시 이어듣기·완료 표시용). */
    @GetMapping("/api/courses/{courseId}/lesson-progress")
    public List<LessonProgressResponse> listForCourse(@PathVariable UUID courseId,
                                                      @AuthenticationPrincipal Jwt jwt) {
        return service.listForCourse(courseId, jwt.getSubject()).stream()
                .map(LessonProgressResponse::from).toList();
    }
}
