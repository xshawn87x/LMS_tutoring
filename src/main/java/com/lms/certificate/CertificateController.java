package com.lms.certificate;

import com.lms.certificate.dto.CertificateResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 수료증 API. 학습자가 자신의 수료증을 조회한다. CERTIFICATES 기능이 켜진 기관에서만 동작(서비스에서 게이팅).
 */
@RestController
public class CertificateController {

    private final CertificateService service;

    public CertificateController(CertificateService service) {
        this.service = service;
    }

    /** 내 수료증 목록. */
    @GetMapping("/api/me/certificates")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public List<CertificateResponse> myCertificates(@AuthenticationPrincipal Jwt jwt) {
        return service.myCertificates(jwt.getSubject());
    }

    /** 특정 과정의 내 수료증 (없으면 본문 없이 200/null). */
    @GetMapping("/api/courses/{courseId}/certificate")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public CertificateResponse forCourse(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        return service.getForCourse(courseId, jwt.getSubject());
    }
}
