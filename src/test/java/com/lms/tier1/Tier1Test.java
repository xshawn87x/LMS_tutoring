package com.lms.tier1;

import com.lms.attendance.AttendanceService;
import com.lms.attendance.dto.AttendanceDtos.Entry;
import com.lms.course.Course;
import com.lms.course.CourseService;
import com.lms.course.dto.CourseRequest;
import com.lms.group.GroupService;
import com.lms.group.StudentGroup;
import com.lms.group.dto.GroupDtos.GroupRequest;
import com.lms.material.CourseMaterial;
import com.lms.material.CourseMaterialRepository;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Tier1: 반/기수·소속·출석·자료실·강의노출. */
@SpringBootTest
@Testcontainers
class Tier1Test {

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

    @Autowired GroupService groupService;
    @Autowired AttendanceService attendanceService;
    @Autowired CourseService courseService;
    @Autowired CourseMaterialRepository materialRepository;

    @Test
    void 반_생성_학생배정_출석기록() {
        TenantContext.set(TENANT_A);
        StudentGroup g = groupService.create(new GroupRequest("초등 3학년 A반", "2026-1기", LocalDate.now(), LocalDate.now().plusMonths(3)));
        groupService.addMember(g.getId(), "kid1@acme");
        groupService.addMember(g.getId(), "kid2@acme");
        assertThat(groupService.members(g.getId())).hasSize(2);

        LocalDate d = LocalDate.now();
        attendanceService.mark(g.getId(), d, List.of(
                new Entry("kid1@acme", "PRESENT", null),
                new Entry("kid2@acme", "LATE", "10분 지각")));
        assertThat(attendanceService.forGroupDate(g.getId(), d)).hasSize(2);

        // 재기록(upsert) — 같은 날 결석으로 정정, 행 수 유지
        attendanceService.mark(g.getId(), d, List.of(new Entry("kid1@acme", "ABSENT", null)));
        assertThat(attendanceService.forGroupDate(g.getId(), d)).hasSize(2);
        assertThat(attendanceService.forStudent("kid1@acme").get(0).getStatus().name()).isEqualTo("ABSENT");
    }

    @Test
    void 반은_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        groupService.create(new GroupRequest("A 전용 반", null, null, null));
        int aCount = groupService.list().size();

        TenantContext.set(TENANT_B);
        assertThat(groupService.list()).noneMatch(x -> x.getName().equals("A 전용 반"));
        assertThat(aCount).isGreaterThan(0);
    }

    @Test
    void 자료실_업로드_조회_삭제() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        CourseMaterial m = materialRepository.save(new CourseMaterial(courseId, "1주차 교재", "/media/x.pdf", "inst@acme"));
        assertThat(materialRepository.findByCourseIdOrderByCreatedAtDesc(courseId)).anyMatch(x -> x.getId().equals(m.getId()));
        materialRepository.delete(m);
        assertThat(materialRepository.findById(m.getId())).isEmpty();
    }

    @Test
    void 강의_비공개면_공개목록에서_제외된다() {
        TenantContext.set(TENANT_A);
        Course c = courseService.create(new CourseRequest("숨김 강의", "x", null, 0));
        assertThat(courseService.findVisible()).anyMatch(x -> x.getId().equals(c.getId()));  // 기본 공개

        courseService.setPublished(c.getId(), false);
        assertThat(courseService.findVisible()).noneMatch(x -> x.getId().equals(c.getId()));  // 비공개 제외
        assertThat(courseService.findAll()).anyMatch(x -> x.getId().equals(c.getId()));       // 전체엔 존재
    }
}
