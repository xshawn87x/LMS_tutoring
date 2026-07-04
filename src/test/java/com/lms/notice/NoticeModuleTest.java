package com.lms.notice;

import com.lms.course.CourseService;
import com.lms.notice.dto.NoticeDtos.NoticeRequest;
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

/** 공지: 학원/강의 공지 생성·조회·핀 정렬·테넌트 격리. */
@SpringBootTest
@Testcontainers
class NoticeModuleTest {

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

    @Autowired NoticeService noticeService;
    @Autowired CourseService courseService;

    @Test
    void 학원_공지_생성후_조회되고_핀이_위로_온다() {
        TenantContext.set(TENANT_A);
        noticeService.createAcademy(new NoticeRequest("일반 공지", "내용", false), "admin@acme");
        noticeService.createAcademy(new NoticeRequest("중요 공지", "내용", true), "admin@acme");

        var list = noticeService.listAcademy();
        assertThat(list).extracting(com.lms.notice.Notice::getTitle).contains("일반 공지", "중요 공지");
        assertThat(list.get(0).isPinned()).isTrue();   // 핀 공지가 먼저
    }

    @Test
    void 강의_공지는_해당_강의에만_보인다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        noticeService.createCourse(courseId, new NoticeRequest("강의 공지", "본문", false), "inst@acme");

        assertThat(noticeService.listCourse(courseId)).extracting(com.lms.notice.Notice::getTitle).contains("강의 공지");
    }

    @Test
    void 공지는_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        noticeService.createAcademy(new NoticeRequest("A 전용 공지", "x", false), "admin@acme");
        int aCount = noticeService.listAcademy().size();

        TenantContext.set(TENANT_B);
        assertThat(noticeService.listAcademy()).extracting(com.lms.notice.Notice::getTitle).doesNotContain("A 전용 공지");
        assertThat(aCount).isGreaterThan(0);
    }

    @Test
    void 공지_수정_삭제() {
        TenantContext.set(TENANT_A);
        var n = noticeService.createAcademy(new NoticeRequest("원본", "x", false), "admin@acme");
        noticeService.update(n.getId(), new NoticeRequest("수정됨", "y", true));
        assertThat(noticeService.listAcademy()).anyMatch(x -> x.getTitle().equals("수정됨") && x.isPinned());

        noticeService.delete(n.getId());
        assertThat(noticeService.listAcademy()).noneMatch(x -> x.getId().equals(n.getId()));
    }
}
