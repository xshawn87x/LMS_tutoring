package com.lms.course.dto;

import com.lms.course.Course;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        UUID tenantId,
        String title,
        String description,
        String categoryCode,
        Integer level,
        boolean published,
        int tuitionFee,
        OffsetDateTime createdAt
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTenantId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategoryCode(),
                course.getLevel(),
                course.isPublished(),
                course.getTuitionFee(),
                course.getCreatedAt());
    }
}
