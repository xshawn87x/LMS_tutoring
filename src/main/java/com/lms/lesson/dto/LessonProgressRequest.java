package com.lms.lesson.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record LessonProgressRequest(
        @PositiveOrZero int lastPositionSeconds,
        boolean completed
) {
}
