package com.lms.certificate.dto;

import com.lms.certificate.CourseCompletion;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String certificateNo,
        OffsetDateTime issuedAt
) {
    public static CertificateResponse from(CourseCompletion c, String courseTitle) {
        return new CertificateResponse(c.getId(), c.getCourseId(), courseTitle, c.getCertificateNo(), c.getIssuedAt());
    }
}
