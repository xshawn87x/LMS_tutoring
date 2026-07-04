package com.lms.qna;

import com.lms.course.CourseService;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 강의 Q&A: 질문·답변·해결 처리·테넌트 격리. */
@SpringBootTest
@Testcontainers
class QnaModuleTest {

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

    @Autowired QnaService qnaService;
    @Autowired CourseService courseService;

    @Test
    void 질문_답변_해결_흐름() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();

        var q = qnaService.ask(courseId, "student@acme", "이거 어떻게 하나요?", "본문");
        assertThat(qnaService.listQuestions(courseId)).anyMatch(s -> s.id().equals(q.getId()) && s.answerCount() == 0);

        qnaService.answer(q.getId(), "inst@acme", "이렇게 하세요");
        var thread = qnaService.getThread(q.getId());
        assertThat(thread.answers()).hasSize(1);
        assertThat(thread.resolved()).isFalse();

        qnaService.setResolved(q.getId(), true);
        assertThat(qnaService.getThread(q.getId()).resolved()).isTrue();
    }

    @Test
    void 질문은_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        UUID courseA = courseService.findAll().get(0).getId();
        qnaService.ask(courseA, "student@acme", "A 질문", "x");

        TenantContext.set(TENANT_B);
        UUID courseB = courseService.findAll().get(0).getId();
        assertThat(qnaService.listQuestions(courseB)).noneMatch(s -> s.title().equals("A 질문"));
    }
}
