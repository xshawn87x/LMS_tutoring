package com.lms.onboarding;

import com.lms.admin.MemberService;
import com.lms.attendance.AttendanceService;
import com.lms.attendance.dto.AttendanceDtos.Entry;
import com.lms.auth.AppUser;
import com.lms.auth.Tenant;
import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.group.GroupService;
import com.lms.group.StudentGroup;
import com.lms.group.dto.GroupDtos.GroupRequest;
import com.lms.guardian.GuardianService;
import com.lms.notification.Notification;
import com.lms.notification.NotificationService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 학원 자가 개설(온보딩) + 결석 시 학부모 자동 알림. */
@SpringBootTest
@Testcontainers
class SegmentFeaturesTest {

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

    @Autowired OnboardingService onboardingService;
    @Autowired MemberService memberService;
    @Autowired GuardianService guardianService;
    @Autowired GroupService groupService;
    @Autowired AttendanceService attendanceService;
    @Autowired NotificationService notificationService;

    @Test
    void 학원을_자가_개설하면_기관과_관리자가_생긴다() {
        long n = System.nanoTime();
        String org = "acad" + n;
        Tenant tenant = onboardingService.createTenant(org, "새 학원");
        assertThat(tenant.getOrgCode()).isEqualTo(org);

        TenantContext.set(tenant.getId());
        AuthResponse resp = onboardingService.createAdmin(tenant, "root" + n + "@new.io", "password123", "원장");
        assertThat(resp.roles()).contains("ADMIN");
        assertThat(resp.orgCode()).isEqualTo(org);
        assertThat(resp.token()).isNotBlank();
    }

    @Test
    void 중복_기관코드나_잘못된_코드는_거부된다() {
        long n = System.nanoTime();
        String org = "dup" + n;
        onboardingService.createTenant(org, "학원");
        assertThatThrownBy(() -> onboardingService.createTenant(org, "또다른"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> onboardingService.createTenant("Bad Code!", "x"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 결석_처리하면_학부모에게_알림이_간다() {
        TenantContext.set(TENANT_A);
        long n = System.nanoTime();
        AppUser child = memberService.create("ch" + n + "@acme", "password123", "자녀", List.of("STUDENT"));
        AppUser parent = memberService.create("pa" + n + "@acme", "password123", "부모", List.of("PARENT"));
        guardianService.link(parent.getEmail(), child.getEmail());
        StudentGroup g = groupService.create(new GroupRequest("반" + n, null, null, null));

        attendanceService.mark(g.getId(), LocalDate.of(2026, 3, 2),
                List.of(new Entry(child.getEmail(), "ABSENT", null)));

        List<Notification> notes = notificationService.myNotifications(parent.getEmail());
        assertThat(notes).anyMatch(x -> x.getTitle().contains("결석"));
        int count = notes.size();

        // 같은 결석을 다시 마킹해도 중복 알림은 없다
        attendanceService.mark(g.getId(), LocalDate.of(2026, 3, 2),
                List.of(new Entry(child.getEmail(), "ABSENT", null)));
        assertThat(notificationService.myNotifications(parent.getEmail())).hasSize(count);
    }
}
