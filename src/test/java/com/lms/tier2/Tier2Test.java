package com.lms.tier2;

import com.lms.assignment.AssignmentService;
import com.lms.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.lms.counseling.CounselingService;
import com.lms.course.CourseService;
import com.lms.notification.NotificationChannel;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Tier2: 상담 기록/예약 + 인앱 알림 + 발송 스텁 + 과제 채점 알림 연동. */
@SpringBootTest
@Testcontainers
class Tier2Test {

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

    @Autowired CounselingService counselingService;
    @Autowired NotificationService notificationService;
    @Autowired AssignmentService assignmentService;
    @Autowired CourseService courseService;

    @Test
    void 상담기록_예약_상태변경() {
        TenantContext.set(TENANT_A);
        counselingService.addRecord("kid@acme", "teacher@acme", "수업 태도 양호");
        assertThat(counselingService.recordsFor("kid@acme")).hasSize(1);

        var appt = counselingService.requestAppointment("kid@acme", "parent@acme", null, "진로 상담");
        assertThat(appt.getStatus()).isEqualTo("REQUESTED");
        var confirmed = counselingService.setStatus(appt.getId(), "confirmed");
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(counselingService.myAppointments("parent@acme")).hasSize(1);
    }

    @Test
    void 인앱_알림_발송_읽음처리() {
        TenantContext.set(TENANT_A);
        notificationService.dispatch("stu@acme", "공지", "새 공지가 있습니다", NotificationChannel.IN_APP);
        assertThat(notificationService.unreadCount("stu@acme")).isGreaterThanOrEqualTo(1);

        var list = notificationService.myNotifications("stu@acme");
        notificationService.markRead(list.get(0).getId(), "stu@acme");
        assertThat(notificationService.myNotifications("stu@acme").get(0).isRead()).isTrue();
    }

    @Test
    void SMS_카카오는_스텁이라_SIMULATED_이력만_남는다() {
        TenantContext.set(TENANT_A);
        assertThat(notificationService.dispatch("010", "제목", "본문", NotificationChannel.SMS)).isEqualTo("SIMULATED");
        assertThat(notificationService.dispatch("010", "제목", "본문", NotificationChannel.KAKAO)).isEqualTo("SIMULATED");
        assertThat(notificationService.recentLogs(10)).anyMatch(l -> l.getChannel() == NotificationChannel.SMS && l.getStatus().equals("SIMULATED"));
    }

    @Test
    void 과제_채점하면_학생에게_알림이_생성된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        var a = assignmentService.create(courseId, new AssignmentRequest("과제", "설명", null, 100));
        var sub = assignmentService.submit(a.getId(), "grade_kid@acme", "답안", null);

        long before = notificationService.unreadCount("grade_kid@acme");
        assignmentService.grade(sub.getId(), 88, "좋아요");
        assertThat(notificationService.myNotifications("grade_kid@acme"))
                .anyMatch(n -> n.getTitle().contains("채점"));
        assertThat(notificationService.unreadCount("grade_kid@acme")).isGreaterThan(before);
    }
}
