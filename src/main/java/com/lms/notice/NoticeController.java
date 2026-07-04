package com.lms.notice;

import com.lms.notice.dto.NoticeDtos.NoticeRequest;
import com.lms.notice.dto.NoticeDtos.NoticeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

/**
 * 공지 API. 조회는 테넌트 내 누구나, 쓰기는 INSTRUCTOR/ADMIN.
 * 학원 공지: /api/notices, 강의 공지: /api/courses/{courseId}/notices.
 */
@RestController
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    // --- 학원 공지 ---

    @GetMapping("/api/notices")
    public List<NoticeResponse> academyNotices() {
        return service.listAcademy().stream().map(NoticeResponse::from).toList();
    }

    @PostMapping("/api/notices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public NoticeResponse createAcademy(@Valid @RequestBody NoticeRequest request, @AuthenticationPrincipal Jwt jwt) {
        return NoticeResponse.from(service.createAcademy(request, jwt.getSubject()));
    }

    // --- 강의 공지 ---

    @GetMapping("/api/courses/{courseId}/notices")
    public List<NoticeResponse> courseNotices(@PathVariable UUID courseId) {
        return service.listCourse(courseId).stream().map(NoticeResponse::from).toList();
    }

    @PostMapping("/api/courses/{courseId}/notices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public NoticeResponse createCourse(@PathVariable UUID courseId, @Valid @RequestBody NoticeRequest request,
                                       @AuthenticationPrincipal Jwt jwt) {
        return NoticeResponse.from(service.createCourse(courseId, request, jwt.getSubject()));
    }

    // --- 공통 수정/삭제 ---

    @PutMapping("/api/notices/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public NoticeResponse update(@PathVariable UUID id, @Valid @RequestBody NoticeRequest request) {
        return NoticeResponse.from(service.update(id, request));
    }

    @DeleteMapping("/api/notices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
