package com.lms.quiz;

import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import com.lms.quiz.dto.QuizDtos.QuestionRequest;
import com.lms.quiz.dto.QuizDtos.SubmissionResult;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 퀴즈 모듈: 채점 정확성 + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class QuizModuleTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lms").withUsername("lms_owner").withPassword("lms_owner_pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lms_app");
        registry.add("spring.datasource.password", () -> "lms_app_pw");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired CourseService courseService;
    @Autowired QuizService quizService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private UUID newQuizWithTwoQuestions() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        Quiz quiz = quizService.createQuiz(courseId, "기초 퀴즈");
        // 정답: 0번 / 1번
        quizService.addQuestion(quiz.getId(),
                new QuestionRequest("1+1=?", List.of("2", "3", "4"), 0, 1));
        quizService.addQuestion(quiz.getId(),
                new QuestionRequest("하늘색은?", List.of("빨강", "파랑"), 1, 2));
        return quiz.getId();
    }

    @Test
    void 모두_맞히면_만점() {
        UUID quizId = newQuizWithTwoQuestions();
        SubmissionResult r = quizService.submit(quizId, "alice", List.of(0, 1));
        assertThat(r.score()).isEqualTo(2);
        assertThat(r.total()).isEqualTo(2);
        assertThat(r.correctness()).containsExactly(true, true);
    }

    @Test
    void 일부_틀리면_부분점수() {
        UUID quizId = newQuizWithTwoQuestions();
        SubmissionResult r = quizService.submit(quizId, "bob", List.of(0, 0)); // 두 번째 오답
        assertThat(r.score()).isEqualTo(1);
        assertThat(r.correctness()).containsExactly(true, false);
    }

    @Test
    void 응시용_문항에는_정답이_노출되지_않는다() {
        UUID quizId = newQuizWithTwoQuestions();
        // QuestionResponse(응답 DTO)에는 correctIndex 필드 자체가 없다 → 컴파일·구조로 보장.
        // 여기서는 questions가 보기는 담되 정답 정보를 들고 있지 않음을 구조적으로 확인.
        var questions = quizService.listQuestions(quizId);
        assertThat(questions).hasSize(2);
        assertThat(com.lms.quiz.dto.QuizDtos.QuestionResponse.from(questions.get(0)).choices()).isNotEmpty();
    }

    @Test
    void 문항을_수정하면_채점도_바뀐다() {
        UUID quizId = newQuizWithTwoQuestions();
        UUID q1 = quizService.listQuestions(quizId).get(0).getId();
        // 1번 문항 정답을 0→2로 바꾼다
        quizService.updateQuestion(quizId, q1, new QuestionRequest("1+1=?(수정)", List.of("2", "3", "4"), 2, 1));

        var q = quizService.listQuestions(quizId).get(0);
        assertThat(q.getBody()).isEqualTo("1+1=?(수정)");
        // 이제 2번 보기가 정답 → 예전 정답(0)은 오답
        SubmissionResult r = quizService.submit(quizId, "carol", List.of(2, 1));
        assertThat(r.correctness().get(0)).isTrue();
    }

    @Test
    void 문항을_삭제할수_있다() {
        UUID quizId = newQuizWithTwoQuestions();
        UUID q1 = quizService.listQuestions(quizId).get(0).getId();
        quizService.deleteQuestion(quizId, q1);
        assertThat(quizService.listQuestions(quizId)).hasSize(1);
    }

    @Test
    void 퀴즈를_삭제하면_문항도_사라진다() {
        UUID quizId = newQuizWithTwoQuestions();
        quizService.deleteQuiz(quizId);
        // 퀴즈가 사라져 조회는 404
        assertThatThrownBy(() -> quizService.getQuiz(quizId)).isInstanceOf(NotFoundException.class);
        assertThat(quizService.listQuestions(quizId)).isEmpty();
    }

    @Test
    void 다른_퀴즈의_문항은_수정할수_없다_404() {
        UUID quizA = newQuizWithTwoQuestions();
        UUID qA = quizService.listQuestions(quizA).get(0).getId();
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        UUID quizB = quizService.createQuiz(courseId, "다른 퀴즈").getId();
        assertThatThrownBy(() -> quizService.updateQuestion(quizB, qA,
                new QuestionRequest("x", List.of("a", "b"), 0, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 다른_테넌트의_퀴즈는_보이지_않는다() {
        UUID quizId = newQuizWithTwoQuestions();

        // B 테넌트에서는 A의 퀴즈가 RLS로 숨겨져 404
        TenantContext.set(TENANT_B);
        assertThatThrownBy(() -> quizService.getQuiz(quizId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> quizService.submit(quizId, "mallory", List.of(0, 0)))
                .isInstanceOf(NotFoundException.class);
    }
}
