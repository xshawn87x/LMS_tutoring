package com.lms.guardian;

import com.lms.admin.MemberService;
import com.lms.auth.AppUser;
import com.lms.course.CourseService;
import com.lms.enrollment.EnrollmentService;
import com.lms.error.BadRequestException;
import com.lms.error.ForbiddenException;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.List;
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

/** 학부모-자녀 연결 + 자녀 학습현황 조회(연결된 경우만). */
@SpringBootTest
@Testcontainers
class GuardianModuleTest {

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

    @Autowired GuardianService guardianService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired CourseService courseService;
    @Autowired MemberService memberService;

    /** 실제 학생/학부모 계정을 만든다(연결은 실계정+역할을 요구한다). */
    private AppUser account(String prefix, String role) {
        return memberService.create(prefix + "_" + System.nanoTime() + "@acme", "password123", prefix, List.of(role));
    }

    @Test
    void 연결된_자녀의_수강현황을_본다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        AppUser child = account("child", "STUDENT");
        enrollmentService.enroll(courseId, child.getEmail());
        AppUser parent = account("parent", "PARENT");

        guardianService.link(parent.getEmail(), child.getEmail());

        assertThat(guardianService.childrenOf(parent.getEmail())).contains(child.getEmail());
        assertThat(guardianService.childEnrollments(parent.getEmail(), child.getEmail())).hasSize(1);
    }

    @Test
    void 연결되지_않은_자녀는_조회할_수_없다() {
        TenantContext.set(TENANT_A);
        assertThatThrownBy(() -> guardianService.childEnrollments("stranger@acme", "someone@acme"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void 존재하지_않는_계정은_연결할_수_없다() {
        TenantContext.set(TENANT_A);
        AppUser parent = account("parent", "PARENT");
        assertThatThrownBy(() -> guardianService.link(parent.getEmail(), "ghost@acme"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 역할이_맞지_않으면_연결할_수_없다() {
        TenantContext.set(TENANT_A);
        AppUser parent = account("parent", "PARENT");
        AppUser notStudent = account("teacher", "INSTRUCTOR");
        assertThatThrownBy(() -> guardianService.link(parent.getEmail(), notStudent.getEmail()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 회원을_삭제하면_연결도_정리된다() {
        TenantContext.set(TENANT_A);
        AppUser child = account("child", "STUDENT");
        AppUser parent = account("parent", "PARENT");
        guardianService.link(parent.getEmail(), child.getEmail());
        assertThat(guardianService.childrenOf(parent.getEmail())).contains(child.getEmail());

        memberService.delete(child.getId());

        assertThat(guardianService.childrenOf(parent.getEmail())).doesNotContain(child.getEmail());
    }
}
