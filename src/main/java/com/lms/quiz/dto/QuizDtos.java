package com.lms.quiz.dto;

import com.lms.quiz.Question;
import com.lms.quiz.Quiz;
import com.lms.quiz.QuizSubmission;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 퀴즈/문항/제출 관련 요청·응답 DTO 모음. 응답 DTO는 모두 정적 {@code from(...)} 팩토리로 엔티티에서 만든다. */
public final class QuizDtos {

    private QuizDtos() {
    }

    // --- 요청 ---

    public record QuizRequest(@NotBlank String title) {
    }

    public record QuestionRequest(
            @NotBlank String body,
            @NotEmpty List<@NotBlank String> choices,
            @PositiveOrZero int correctIndex,
            @PositiveOrZero int orderNo
    ) {
    }

    public record SubmitRequest(
            @NotNull @NotEmpty List<@Min(0) Integer> answers
    ) {
    }

    // --- 응답 ---

    public record QuizResponse(UUID id, UUID courseId, String title) {
        public static QuizResponse from(Quiz quiz) {
            return new QuizResponse(quiz.getId(), quiz.getCourseId(), quiz.getTitle());
        }
    }

    /** 응시용 문항 — 정답(correctIndex)은 노출하지 않는다. */
    public record QuestionResponse(UUID id, String body, List<String> choices, int orderNo) {
        public static QuestionResponse from(Question question) {
            return new QuestionResponse(
                    question.getId(), question.getBody(), question.getChoices(), question.getOrderNo());
        }
    }

    /** 퀴즈 + 문항 목록 (응시 화면용). */
    public record QuizDetailResponse(UUID id, String title, List<QuestionResponse> questions) {
        public static QuizDetailResponse from(Quiz quiz, List<Question> questions) {
            return new QuizDetailResponse(
                    quiz.getId(),
                    quiz.getTitle(),
                    questions.stream().map(QuestionResponse::from).toList());
        }
    }

    /** 채점 결과 (점수는 계산값이라 엔티티 매핑이 아닌 서비스에서 구성). */
    public record SubmissionResult(
            UUID submissionId,
            int score,
            int total,
            List<Boolean> correctness
    ) {
    }

    /** 내 제출 이력 요약. */
    public record SubmissionSummary(
            UUID id,
            UUID quizId,
            int score,
            int total,
            OffsetDateTime submittedAt
    ) {
        public static SubmissionSummary from(QuizSubmission submission) {
            return new SubmissionSummary(
                    submission.getId(), submission.getQuizId(),
                    submission.getScore(), submission.getTotal(), submission.getSubmittedAt());
        }
    }
}
