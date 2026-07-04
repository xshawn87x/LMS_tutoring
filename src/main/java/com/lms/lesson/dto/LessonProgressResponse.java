package com.lms.lesson.dto;

import com.lms.lesson.LessonProgress;

import java.util.UUID;

public record LessonProgressResponse(
        UUID lessonId,
        UUID courseId,
        int lastPositionSeconds,
        boolean completed
) {
    public static LessonProgressResponse from(LessonProgress p) {
        return new LessonProgressResponse(
                p.getLessonId(),
                p.getCourseId(),
                p.getLastPositionSeconds(),
                p.isCompleted());
    }
}
