package com.lms.quiz;

import com.lms.course.CourseService;
import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.quiz.dto.QuizDtos.QuestionRequest;
import com.lms.quiz.dto.QuizDtos.SubmissionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final FeatureService featureService;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository,
                       QuizSubmissionRepository submissionRepository, CourseService courseService,
                       FeatureService featureService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.courseService = courseService;
        this.featureService = featureService;
    }

    public Quiz createQuiz(UUID courseId, String title) {
        featureService.requireEnabled(Feature.QUIZZES);
        courseService.requireExists(courseId);
        return quizRepository.save(new Quiz(courseId, title));
    }

    @Transactional(readOnly = true)
    public List<Quiz> listQuizzes(UUID courseId) {
        featureService.requireEnabled(Feature.QUIZZES);
        courseService.requireExists(courseId);
        return quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Quiz getQuiz(UUID quizId) {
        featureService.requireEnabled(Feature.QUIZZES);
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("퀴즈를 찾을 수 없습니다: " + quizId));
    }

    @Transactional(readOnly = true)
    public List<Question> listQuestions(UUID quizId) {
        return questionRepository.findByQuizIdOrderByOrderNoAsc(quizId);
    }

    public Question addQuestion(UUID quizId, QuestionRequest req) {
        getQuiz(quizId); // 퀴즈가 현재 테넌트에 있어야 함 (RLS → 없으면 404)
        validateCorrectIndex(req);
        return questionRepository.save(
                new Question(quizId, req.body(), req.choices(), req.correctIndex(), req.orderNo()));
    }

    public Question updateQuestion(UUID quizId, UUID questionId, QuestionRequest req) {
        getQuiz(quizId);
        validateCorrectIndex(req);
        Question q = requireQuestionInQuiz(quizId, questionId);
        q.update(req.body(), req.choices(), req.correctIndex(), req.orderNo());
        return q;
    }

    public void deleteQuestion(UUID quizId, UUID questionId) {
        getQuiz(quizId);
        questionRepository.delete(requireQuestionInQuiz(quizId, questionId));
    }

    /** 퀴즈 삭제 — 문항·제출은 FK ON DELETE CASCADE로 함께 삭제된다. */
    public void deleteQuiz(UUID quizId) {
        quizRepository.delete(getQuiz(quizId));
    }

    private void validateCorrectIndex(QuestionRequest req) {
        if (req.correctIndex() >= req.choices().size()) {
            throw new BadRequestException("correctIndex가 보기 범위를 벗어났습니다");
        }
    }

    private Question requireQuestionInQuiz(UUID quizId, UUID questionId) {
        return questionRepository.findById(questionId)
                .filter(q -> q.getQuizId().equals(quizId))
                .orElseThrow(() -> new NotFoundException("문항을 찾을 수 없습니다"));
    }

    /** 제출을 채점하고 결과를 저장한다. 정답은 서버에서만 비교한다. */
    public SubmissionResult submit(UUID quizId, String studentId, List<Integer> answers) {
        getQuiz(quizId);
        List<Question> questions = questionRepository.findByQuizIdOrderByOrderNoAsc(quizId);
        if (questions.isEmpty()) {
            throw new BadRequestException("문항이 없는 퀴즈입니다");
        }

        List<Boolean> correctness = new ArrayList<>(questions.size());
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            Integer answer = (i < answers.size()) ? answers.get(i) : null;
            boolean correct = answer != null && questions.get(i).isCorrect(answer);
            correctness.add(correct);
            if (correct) {
                score++;
            }
        }

        QuizSubmission saved = submissionRepository.save(
                new QuizSubmission(quizId, studentId, score, questions.size()));
        return new SubmissionResult(saved.getId(), score, questions.size(), correctness);
    }

    @Transactional(readOnly = true)
    public List<QuizSubmission> mySubmissions(String studentId) {
        featureService.requireEnabled(Feature.QUIZZES);
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
    }
}
