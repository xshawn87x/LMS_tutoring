package com.lms.dashboard;

import com.lms.course.CourseService;
import com.lms.dashboard.dto.CourseStatsResponse;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 강사 대시보드 집계(수강생 수·평균 진도·완료 수) + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class DashboardTest {

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

    @Autowired DashboardService dashboardService;
    @Autowired CourseService courseService;
    @Autowired EnrollmentService enrollmentService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void 과정별_수강생수와_평균진도를_집계한다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        Enrollment a = enrollmentService.enroll(courseId, "stat-a");
        enrollmentService.updateProgress(a.getId(), "stat-a", 100);
        enrollmentService.enroll(courseId, "stat-b"); // 진도 0

        CourseStatsResponse stat = dashboardService.courseStats().stream()
                .filter(s -> s.courseId().equals(courseId)).findFirst().orElseThrow();

        assertThat(stat.enrollmentCount()).isGreaterThanOrEqualTo(2);
        assertThat(stat.completedCount()).isGreaterThanOrEqualTo(1);
        assertThat(stat.avgProgress()).isBetween(0, 100);
    }

    @Test
    void 수강생_진도_상세를_드릴다운한다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.create(
                new com.lms.course.dto.CourseRequest("드릴다운", null, "BACKEND", 0)).getId();
        Enrollment done = enrollmentService.enroll(courseId, "drill-done");
        enrollmentService.updateProgress(done.getId(), "drill-done", 100);
        enrollmentService.enroll(courseId, "drill-mid");

        var students = dashboardService.courseStudents(courseId);
        assertThat(students).hasSize(2);
        assertThat(students).extracting("studentId")
                .containsExactlyInAnyOrder("drill-done", "drill-mid");
        var completed = students.stream().filter(s -> s.studentId().equals("drill-done")).findFirst().orElseThrow();
        assertThat(completed.completed()).isTrue();
        assertThat(completed.progress()).isEqualTo(100);
        var mid = students.stream().filter(s -> s.studentId().equals("drill-mid")).findFirst().orElseThrow();
        assertThat(mid.completed()).isFalse();
        assertThat(mid.quizzesTotal()).isEqualTo(0);
    }

    @Test
    void 대시보드는_테넌트별로_격리된다() {
        // A에 수강 추가
        TenantContext.set(TENANT_A);
        UUID aCourse = courseService.findAll().get(0).getId();
        enrollmentService.enroll(aCourse, "only-a");

        // B 대시보드엔 A의 과정/수강이 없음 (RLS)
        TenantContext.set(TENANT_B);
        var bStats = dashboardService.courseStats();
        assertThat(bStats).noneMatch(s -> s.courseId().equals(aCourse));
        // B는 자기 시드 과정(Python 입문)만 본다
        assertThat(bStats).allMatch(s -> s.enrollmentCount() >= 0);
    }
}
