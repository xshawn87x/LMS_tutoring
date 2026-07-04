package com.lms.guardian;

import com.lms.course.CourseService;
import com.lms.enrollment.EnrollmentService;
import com.lms.error.ForbiddenException;
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

    @Test
    void 연결된_자녀의_수강현황을_본다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        String child = "child_" + System.nanoTime() + "@acme";
        enrollmentService.enroll(courseId, child);

        String parent = "parent_" + System.nanoTime() + "@acme";
        guardianService.link(parent, child);

        assertThat(guardianService.childrenOf(parent)).contains(child);
        assertThat(guardianService.childEnrollments(parent, child)).hasSize(1);
    }

    @Test
    void 연결되지_않은_자녀는_조회할_수_없다() {
        TenantContext.set(TENANT_A);
        assertThatThrownBy(() -> guardianService.childEnrollments("stranger@acme", "someone@acme"))
                .isInstanceOf(ForbiddenException.class);
    }
}
