package com.lms.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record LessonRequest(
        @NotBlank String title,
        String content,
        String videoUrl,
        @PositiveOrZero int orderNo
) {
}
