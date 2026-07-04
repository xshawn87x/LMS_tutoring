package com.lms.assignment;

import com.lms.assignment.dto.AssignmentDtos.AssignmentRequest;
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

/** 과제: 생성·제출·재제출·채점·테넌트 격리. */
@SpringBootTest
@Testcontainers
class AssignmentModuleTest {

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

    @Autowired AssignmentService service;
    @Autowired CourseService courseService;

    @Test
    void 생성_제출_재제출_채점() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        var a = service.create(courseId, new AssignmentRequest("1주차 과제", "설명", null, 100));

        var sub = service.submit(a.getId(), "stu@acme", "제 답안입니다", null);
        assertThat(service.mySubmission(a.getId(), "stu@acme").getTextAnswer()).isEqualTo("제 답안입니다");

        // 재제출 → 같은 행 갱신(제출 1건 유지)
        service.submit(a.getId(), "stu@acme", "수정한 답안", null);
        assertThat(service.submissions(a.getId())).hasSize(1);
        assertThat(service.mySubmission(a.getId(), "stu@acme").getTextAnswer()).isEqualTo("수정한 답안");

        // 채점
        service.grade(sub.getId(), 90, "잘했어요");
        var graded = service.mySubmission(a.getId(), "stu@acme");
        assertThat(graded.getScore()).isEqualTo(90);
        assertThat(graded.getFeedback()).isEqualTo("잘했어요");
    }

    @Test
    void 과제는_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        UUID courseA = courseService.findAll().get(0).getId();
        service.create(courseA, new AssignmentRequest("A 과제", "x", null, 100));

        TenantContext.set(TENANT_B);
        UUID courseB = courseService.findAll().get(0).getId();
        assertThat(service.list(courseB)).noneMatch(x -> x.getTitle().equals("A 과제"));
    }
}
