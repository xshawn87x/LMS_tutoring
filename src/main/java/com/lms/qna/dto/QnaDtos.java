package com.lms.qna.dto;

import com.lms.qna.CourseAnswer;
import com.lms.qna.CourseQuestion;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class QnaDtos {

    private QnaDtos() {
    }

    public record QuestionRequest(@NotBlank String title, String body) {
    }

    public record AnswerRequest(@NotBlank String body) {
    }

    public record AnswerResponse(UUID id, String author, String body, OffsetDateTime createdAt) {
        public static AnswerResponse from(CourseAnswer a) {
            return new AnswerResponse(a.getId(), a.getAuthor(), a.getBody(), a.getCreatedAt());
        }
    }

    public record QuestionSummary(
            UUID id, UUID courseId, String author, String title,
            boolean resolved, int answerCount, OffsetDateTime createdAt) {
        public static QuestionSummary of(CourseQuestion q, int answerCount) {
            return new QuestionSummary(q.getId(), q.getCourseId(), q.getAuthor(), q.getTitle(),
                    q.isResolved(), answerCount, q.getCreatedAt());
        }
    }

    public record ThreadResponse(
            UUID id, UUID courseId, String author, String title, String body,
            boolean resolved, OffsetDateTime createdAt, List<AnswerResponse> answers) {
        public static ThreadResponse of(CourseQuestion q, List<CourseAnswer> answers) {
            return new ThreadResponse(q.getId(), q.getCourseId(), q.getAuthor(), q.getTitle(), q.getBody(),
                    q.isResolved(), q.getCreatedAt(), answers.stream().map(AnswerResponse::from).toList());
        }
    }
}
