package com.lms.course;

import com.lms.course.dto.CourseRequest;
import com.lms.error.NotFoundException;
import com.lms.lesson.Lesson;
import com.lms.lesson.LessonService;
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

/** 콘텐츠 관리: 과정/레슨 수정·삭제(+순서) 검증. */
@SpringBootTest
@Testcontainers
class ContentManagementTest {

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

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void 과정을_수정하고_삭제할수_있다() {
        TenantContext.set(TENANT_A);
        UUID id = courseService.create(new CourseRequest("원본", "설명", "BACKEND", 0)).getId();

        courseService.update(id, new CourseRequest("수정됨", "새 설명", "DATA", 2));
        Course updated = courseService.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정됨");
        assertThat(updated.getCategoryCode()).isEqualTo("DATA");
        assertThat(updated.getLevel()).isEqualTo(2);

        courseService.delete(id);
        assertThat(courseService.findById(id)).isEmpty();
    }

    @Test
    void 과정_삭제시_레슨도_함께_사라진다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.create(new CourseRequest("삭제대상", null, "BACKEND", 0)).getId();
        lessonService.add(courseId, new LessonRequest("1강", "내용", null, 1));
        assertThat(lessonService.listByCourse(courseId)).hasSize(1);

        courseService.delete(courseId);
        // 과정이 사라졌으므로 레슨 조회는 404 (과정 가드)
        assertThatThrownBy(() -> lessonService.listByCourse(courseId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 레슨을_수정하고_삭제할수_있다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.create(new CourseRequest("레슨관리", null, "BACKEND", 0)).getId();
        Lesson l1 = lessonService.add(courseId, new LessonRequest("1강", "a", null, 1));
        lessonService.add(courseId, new LessonRequest("2강", "b", null, 2));

        // 수정 (제목/영상/순서)
        lessonService.update(courseId, l1.getId(), new LessonRequest("1강(수정)", "a2", "https://x/v.mp4", 3));
        Lesson reloaded = lessonService.listByCourse(courseId).stream()
                .filter(l -> l.getId().equals(l1.getId())).findFirst().orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("1강(수정)");
        assertThat(reloaded.getVideoUrl()).isEqualTo("https://x/v.mp4");
        assertThat(reloaded.getOrderNo()).isEqualTo(3);

        // 삭제
        lessonService.delete(courseId, l1.getId());
        assertThat(lessonService.listByCourse(courseId)).hasSize(1);
    }

    @Test
    void 다른_과정의_레슨은_수정할수_없다_404() {
        TenantContext.set(TENANT_A);
        UUID courseA = courseService.create(new CourseRequest("A", null, "BACKEND", 0)).getId();
        UUID courseB = courseService.create(new CourseRequest("B", null, "BACKEND", 0)).getId();
        Lesson l = lessonService.add(courseA, new LessonRequest("1강", null, null, 1));

        // courseB 경로로 courseA의 레슨 수정 시도 → 404
        assertThatThrownBy(() -> lessonService.update(courseB, l.getId(), new LessonRequest("x", null, null, 1)))
                .isInstanceOf(NotFoundException.class);
    }
}
