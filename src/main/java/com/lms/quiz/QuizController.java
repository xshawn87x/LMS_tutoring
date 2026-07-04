package com.lms.quiz;

import com.lms.quiz.dto.QuizDtos.QuestionRequest;
import com.lms.quiz.dto.QuizDtos.QuestionResponse;
import com.lms.quiz.dto.QuizDtos.QuizDetailResponse;
import com.lms.quiz.dto.QuizDtos.QuizRequest;
import com.lms.quiz.dto.QuizDtos.QuizResponse;
import com.lms.quiz.dto.QuizDtos.SubmissionResult;
import com.lms.quiz.dto.QuizDtos.SubmissionSummary;
import com.lms.quiz.dto.QuizDtos.SubmitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) {
        this.service = service;
    }

    // --- 과정 하위: 퀴즈 ---

    @GetMapping("/api/courses/{courseId}/quizzes")
    public List<QuizResponse> listQuizzes(@PathVariable UUID courseId) {
        return service.listQuizzes(courseId).stream().map(QuizResponse::from).toList();
    }

    @PostMapping("/api/courses/{courseId}/quizzes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public QuizResponse createQuiz(@PathVariable UUID courseId, @Valid @RequestBody QuizRequest request) {
        return QuizResponse.from(service.createQuiz(courseId, request.title()));
    }

    // --- 퀴즈 하위: 문항 / 응시 / 제출 ---

    /** 응시용 퀴즈 상세 — 정답은 포함되지 않는다. */
    @GetMapping("/api/quizzes/{quizId}")
    public QuizDetailResponse getQuiz(@PathVariable UUID quizId) {
        Quiz quiz = service.getQuiz(quizId);
        return QuizDetailResponse.from(quiz, service.listQuestions(quizId));
    }

    @PostMapping("/api/quizzes/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public QuestionResponse addQuestion(@PathVariable UUID quizId, @Valid @RequestBody QuestionRequest request) {
        return QuestionResponse.from(service.addQuestion(quizId, request));
    }

    @PutMapping("/api/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public QuestionResponse updateQuestion(@PathVariable UUID quizId, @PathVariable UUID questionId,
                                           @Valid @RequestBody QuestionRequest request) {
        return QuestionResponse.from(service.updateQuestion(quizId, questionId, request));
    }

    @DeleteMapping("/api/quizzes/{quizId}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void deleteQuestion(@PathVariable UUID quizId, @PathVariable UUID questionId) {
        service.deleteQuestion(quizId, questionId);
    }

    @DeleteMapping("/api/quizzes/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void deleteQuiz(@PathVariable UUID quizId) {
        service.deleteQuiz(quizId);
    }

    /** 답안을 제출하고 즉시 채점 결과를 받는다. */
    @PostMapping("/api/quizzes/{quizId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public SubmissionResult submit(@PathVariable UUID quizId,
                                   @Valid @RequestBody SubmitRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return service.submit(quizId, jwt.getSubject(), request.answers());
    }

    /** 내 퀴즈 제출 이력. */
    @GetMapping("/api/quiz-submissions/me")
    public List<SubmissionSummary> mySubmissions(@AuthenticationPrincipal Jwt jwt) {
        return service.mySubmissions(jwt.getSubject()).stream().map(SubmissionSummary::from).toList();
    }
}
