package com.lms.curation;

import com.lms.curation.dto.ContentInsightResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 콘텐츠 분석(큐레이션) API. AI_CURATION 기능이 켜진 기관에서만 동작(서비스에서 게이팅).
 */
@RestController
public class ContentInsightController {

    private final ContentInsightService service;

    public ContentInsightController(ContentInsightService service) {
        this.service = service;
    }

    /** 분석 실행/재실행 (강사·관리자). */
    @PostMapping("/api/courses/{courseId}/insight")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ContentInsightResponse analyze(@PathVariable UUID courseId) {
        return ContentInsightResponse.from(service.analyze(courseId));
    }

    /** 분석 결과 조회. 아직 분석 전이면 본문 없이 200(null). */
    @GetMapping("/api/courses/{courseId}/insight")
    public ContentInsightResponse get(@PathVariable UUID courseId) {
        return service.get(courseId).map(ContentInsightResponse::from).orElse(null);
    }
}
