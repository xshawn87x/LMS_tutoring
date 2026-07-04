package com.lms.enrollment.dto;

import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID courseId,
        String studentId,
        EnrollmentStatus status,
        int progress,
        OffsetDateTime enrolledAt,
        OffsetDateTime updatedAt
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                enrollment.getStudentId(),
                enrollment.getStatus(),
                enrollment.getProgress(),
                enrollment.getEnrolledAt(),
                enrollment.getUpdatedAt());
    }
}
