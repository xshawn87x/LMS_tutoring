package com.lms.lesson.dto;

import com.lms.lesson.Lesson;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LessonResponse(
        UUID id,
        UUID courseId,
        String title,
        String content,
        String videoUrl,
        int orderNo,
        OffsetDateTime createdAt
) {
    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getCourseId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getVideoUrl(),
                lesson.getOrderNo(),
                lesson.getCreatedAt());
    }
}
