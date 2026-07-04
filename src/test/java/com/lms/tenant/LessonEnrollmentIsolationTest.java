package com.lms.tenant;

import com.lms.course.CourseService;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
import com.lms.enrollment.EnrollmentStatus;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.lesson.LessonService;
import com.lms.lesson.dto.LessonRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 레슨/수강신청도 course와 동일하게 RLS로 테넌트 격리됨을 검증한다.
 */
@SpringBootTest
@Testcontainers
class LessonEnrollmentIsolationTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lms")
            .withUsername("lms_owner")
            .withPassword("lms_owner_pw");

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
    @Autowired LessonService lessonService;
    @Autowired EnrollmentService enrollmentService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private UUID aCourseId() {
        TenantContext.set(TENANT_A);
        return courseService.findAll().get(0).getId();
    }

    @Test
    void 레슨은_과정과_같은_테넌트에만_보인다() {
        // 시드 과정엔 V12 레슨이 있으므로, 카운트가 결정적이도록 새 과정을 만들어 검증한다
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.create(
                new com.lms.course.dto.CourseRequest("레슨 격리 테스트", null, "BACKEND", 0)).getId();
        lessonService.add(courseId, new LessonRequest("1강", "내용", null, 1));
        assertThat(lessonService.listByCourse(courseId)).hasSize(1);

        // B는 그 과정 자체가 안 보이므로 레슨 조회는 404
        TenantContext.set(TENANT_B);
        assertThatThrownBy(() -> lessonService.listByCourse(courseId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 수강신청_진도갱신_및_테넌트격리() {
        UUID courseId = aCourseId();

        TenantContext.set(TENANT_A);
        Enrollment e = enrollmentService.enroll(courseId, "alice");
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollmentService.listMine("alice")).hasSize(1);

        // 중복 수강신청은 거부 (409)
        assertThatThrownBy(() -> enrollmentService.enroll(courseId, "alice"))
                .isInstanceOf(ConflictException.class);

        // 진도 100 → 자동 완료
        Enrollment done = enrollmentService.updateProgress(e.getId(), "alice", 100);
        assertThat(done.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(done.getProgress()).isEqualTo(100);

        // B 테넌트에서는 alice의 수강이 보이지 않고, A의 과정에 수강신청도 불가(404)
        TenantContext.set(TENANT_B);
        assertThat(enrollmentService.listMine("alice")).isEmpty();
        assertThatThrownBy(() -> enrollmentService.enroll(courseId, "bob"))
                .isInstanceOf(NotFoundException.class);
    }
}
