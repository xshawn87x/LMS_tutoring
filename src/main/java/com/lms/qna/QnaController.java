package com.lms.qna;

import com.lms.qna.dto.QnaDtos.AnswerRequest;
import com.lms.qna.dto.QnaDtos.AnswerResponse;
import com.lms.qna.dto.QnaDtos.QuestionRequest;
import com.lms.qna.dto.QnaDtos.QuestionSummary;
import com.lms.qna.dto.QnaDtos.ThreadResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 강의 Q&A API. 질문 목록/작성은 강의 하위 경로, 스레드/답변/해결은 질문 하위 경로.
 * 질문 작성=인증 사용자 누구나, 답변·해결·삭제=INSTRUCTOR/ADMIN.
 */
@RestController
public class QnaController {

    private final QnaService service;

    public QnaController(QnaService service) {
        this.service = service;
    }

    @GetMapping("/api/courses/{courseId}/questions")
    public List<QuestionSummary> list(@PathVariable UUID courseId) {
        return service.listQuestions(courseId);
    }

    @PostMapping("/api/courses/{courseId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ThreadResponse ask(@PathVariable UUID courseId, @Valid @RequestBody QuestionRequest request,
                              @AuthenticationPrincipal Jwt jwt) {
        var q = service.ask(courseId, jwt.getSubject(), request.title(), request.body());
        return service.getThread(q.getId());
    }

    @GetMapping("/api/questions/{id}")
    public ThreadResponse thread(@PathVariable UUID id) {
        return service.getThread(id);
    }

    @PostMapping("/api/questions/{id}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public AnswerResponse answer(@PathVariable UUID id, @Valid @RequestBody AnswerRequest request,
                                 @AuthenticationPrincipal Jwt jwt) {
        return AnswerResponse.from(service.answer(id, jwt.getSubject(), request.body()));
    }

    @PostMapping("/api/questions/{id}/resolve")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ThreadResponse resolve(@PathVariable UUID id, @RequestParam(defaultValue = "true") boolean resolved) {
        service.setResolved(id, resolved);
        return service.getThread(id);
    }

    @DeleteMapping("/api/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.deleteQuestion(id);
    }
}
