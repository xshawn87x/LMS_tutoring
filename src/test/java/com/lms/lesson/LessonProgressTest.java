package com.lms.lesson;

import com.lms.course.CourseService;
import com.lms.course.dto.CourseRequest;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.enrollment.EnrollmentService;
import com.lms.enrollment.EnrollmentStatus;
import com.lms.error.NotFoundException;
import com.lms.lesson.dto.LessonProgressRequest;
import com.lms.lesson.dto.LessonRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 학습창 진도: 이어듣기(재생 위치) + 완료 시 수강 진도 자동 재계산 + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class LessonProgressTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
    @Autowired LessonService lessonService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired LessonProgressService progressService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private UUID newCourseWithLessons(int lessonCount) {
        UUID courseId = courseService.create(new CourseRequest("학습창 테스트", null, "BACKEND", 0)).getId();
        for (int i = 1; i <= lessonCount; i++) {
            lessonService.add(courseId, new LessonRequest(i + "강", "내용", null, i));
        }
        return courseId;
    }

    private UUID lessonAt(UUID courseId, int index) {
        return lessonService.listByCourse(courseId).get(index).getId();
    }

    @Test
    void 재생위치가_저장되고_이어듣기로_조회된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(2);
        enrollmentService.enroll(courseId, "alice");
        UUID lesson1 = lessonAt(courseId, 0);

        progressService.save(lesson1, "alice", new LessonProgressRequest(42, false));

        var list = progressService.listForCourse(courseId, "alice");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getLastPositionSeconds()).isEqualTo(42);
        assertThat(list.get(0).isCompleted()).isFalse();
    }

    @Test
    void 일부_레슨_완료시_수강진도가_비율로_재계산된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(4);
        enrollmentService.enroll(courseId, "bob");

        progressService.save(lessonAt(courseId, 0), "bob", new LessonProgressRequest(0, true));
        progressService.save(lessonAt(courseId, 1), "bob", new LessonProgressRequest(0, true));

        Enrollment e = enrollmentRepository.findByCourseIdAndStudentId(courseId, "bob").orElseThrow();
        assertThat(e.getProgress()).isEqualTo(50);
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
    }

    @Test
    void 모든_레슨_완료시_수강진도100_완료처리된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(2);
        enrollmentService.enroll(courseId, "carol");

        progressService.save(lessonAt(courseId, 0), "carol", new LessonProgressRequest(10, true));
        progressService.save(lessonAt(courseId, 1), "carol", new LessonProgressRequest(20, true));

        Enrollment e = enrollmentRepository.findByCourseIdAndStudentId(courseId, "carol").orElseThrow();
        assertThat(e.getProgress()).isEqualTo(100);
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void 완료된_레슨은_위치만_갱신해도_미완료로_내려가지_않는다() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(1);
        enrollmentService.enroll(courseId, "dave");
        UUID lesson1 = lessonAt(courseId, 0);

        progressService.save(lesson1, "dave", new LessonProgressRequest(100, true));
        // 이후 위치만 다시 저장(completed=false) — 한 번 완료된 건 유지
        progressService.save(lesson1, "dave", new LessonProgressRequest(120, false));

        var p = progressService.listForCourse(courseId, "dave").get(0);
        assertThat(p.isCompleted()).isTrue();
        assertThat(p.getLastPositionSeconds()).isEqualTo(120);
    }

    @Test
    void 수강_취소시_수강과_진도가_모두_초기화된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(2);
        Enrollment enr = enrollmentService.enroll(courseId, "erin");
        progressService.save(lessonAt(courseId, 0), "erin", new LessonProgressRequest(0, true));
        assertThat(progressService.listForCourse(courseId, "erin")).hasSize(1);

        enrollmentService.cancel(enr.getId(), "erin");

        assertThat(enrollmentRepository.findByCourseIdAndStudentId(courseId, "erin")).isEmpty();
        assertThat(progressService.listForCourse(courseId, "erin")).isEmpty();
    }

    @Test
    void 남의_수강은_취소할수_없다_404() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(1);
        Enrollment enr = enrollmentService.enroll(courseId, "frank");
        assertThatThrownBy(() -> enrollmentService.cancel(enr.getId(), "intruder"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 수강신청_없이_진도저장은_404() {
        TenantContext.set(TENANT_A);
        UUID courseId = newCourseWithLessons(1);
        UUID lesson1 = lessonAt(courseId, 0);

        assertThatThrownBy(() -> progressService.save(lesson1, "stranger", new LessonProgressRequest(10, true)))
                .isInstanceOf(NotFoundException.class);
    }
}
