package com.lms.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CourseRequest(
        @NotBlank String title,
        String description,
        String categoryCode,
        @Min(0) @Max(3) Integer level
) {
}
