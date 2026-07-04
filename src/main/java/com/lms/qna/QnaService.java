package com.lms.qna;

import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import com.lms.qna.dto.QnaDtos.QuestionSummary;
import com.lms.qna.dto.QnaDtos.ThreadResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 강의 Q&A. 질문(수강생) + 답변(강사/관리자). RLS로 테넌트 격리.
 * 쓰기 권한은 컨트롤러에서 제한(질문=인증자, 답변/해결처리=INSTRUCTOR/ADMIN).
 */
@Service
@Transactional
public class QnaService {

    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final CourseService courseService;

    public QnaService(CourseQuestionRepository questionRepository, CourseAnswerRepository answerRepository,
                      CourseService courseService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public List<QuestionSummary> listQuestions(UUID courseId) {
        courseService.requireExists(courseId);
        List<CourseQuestion> questions = questionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        // 답변 수를 한 번에 집계 (N+1 방지)
        java.util.Map<UUID, Integer> answerCounts = new java.util.HashMap<>();
        if (!questions.isEmpty()) {
            List<UUID> ids = questions.stream().map(CourseQuestion::getId).toList();
            for (CourseAnswer a : answerRepository.findByQuestionIdIn(ids)) {
                answerCounts.merge(a.getQuestionId(), 1, Integer::sum);
            }
        }
        return questions.stream()
                .map(q -> QuestionSummary.of(q, answerCounts.getOrDefault(q.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ThreadResponse getThread(UUID questionId) {
        CourseQuestion q = requireQuestion(questionId);
        return ThreadResponse.of(q, answerRepository.findByQuestionIdOrderByCreatedAtAsc(questionId));
    }

    public CourseQuestion ask(UUID courseId, String author, String title, String body) {
        courseService.requireExists(courseId);
        return questionRepository.save(new CourseQuestion(courseId, author, title, body));
    }

    public CourseAnswer answer(UUID questionId, String author, String body) {
        requireQuestion(questionId);   // 존재/테넌트 확인
        return answerRepository.save(new CourseAnswer(questionId, author, body));
    }

    public void setResolved(UUID questionId, boolean resolved) {
        requireQuestion(questionId).setResolved(resolved);
    }

    public void deleteQuestion(UUID questionId) {
        questionRepository.delete(requireQuestion(questionId));   // 답변은 FK CASCADE
    }

    private CourseQuestion requireQuestion(UUID questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다: " + questionId));
    }
}
