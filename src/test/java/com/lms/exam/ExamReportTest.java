package com.lms.exam;

import com.lms.admin.MemberService;
import com.lms.auth.AppUser;
import com.lms.course.CourseService;
import com.lms.enrollment.EnrollmentService;
import com.lms.error.BadRequestException;
import com.lms.error.ForbiddenException;
import com.lms.exam.dto.ExamDtos.ExamRequest;
import com.lms.exam.dto.ExamDtos.ScoreEntry;
import com.lms.exam.dto.ExamDtos.StudentScore;
import com.lms.guardian.GuardianService;
import com.lms.report.dto.ReportDtos.StudentReport;
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

/** 시험/성적(추이) + 학부모 리포트 집계 + 학부모 접근 통제. */
@SpringBootTest
@Testcontainers
class ExamReportTest {

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

    @Autowired ExamService examService;
    @Autowired GuardianService guardianService;
    @Autowired MemberService memberService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired CourseService courseService;

    private AppUser account(String prefix, String role) {
        return memberService.create(prefix + "_" + System.nanoTime() + "@acme", "password123", prefix, List.of(role));
    }

    @Test
    void 성적을_입력하면_추이가_시행일순으로_계산된다() {
        TenantContext.set(TENANT_A);
        AppUser student = account("stu", "STUDENT");

        UUID mid = examService.create(new ExamRequest("3월 모의고사", "수학", LocalDate.of(2026, 3, 10), 100, null)).getId();
        UUID fin = examService.create(new ExamRequest("6월 모의고사", "수학", LocalDate.of(2026, 6, 10), 100, null)).getId();
        // 일부러 나중 시험을 먼저 입력 — 그래도 추이는 시행일순이어야 한다
        examService.recordScores(fin, List.of(new ScoreEntry(student.getEmail(), 92, "향상")));
        examService.recordScores(mid, List.of(new ScoreEntry(student.getEmail(), 80, null)));

        List<StudentScore> trend = examService.studentScores(student.getEmail());
        assertThat(trend).hasSize(2);
        assertThat(trend.get(0).examDate()).isBefore(trend.get(1).examDate()); // 오름차순
        assertThat(trend.get(0).score()).isEqualTo(80);
        assertThat(trend.get(1).percent()).isEqualTo(92);
    }

    @Test
    void 성적_수정은_덮어쓴다() {
        TenantContext.set(TENANT_A);
        AppUser student = account("stu", "STUDENT");
        UUID exam = examService.create(new ExamRequest("쪽지시험", "영어", LocalDate.of(2026, 4, 1), 50, null)).getId();
        examService.recordScores(exam, List.of(new ScoreEntry(student.getEmail(), 40, null)));
        examService.recordScores(exam, List.of(new ScoreEntry(student.getEmail(), 45, "재채점")));

        List<StudentScore> trend = examService.studentScores(student.getEmail());
        assertThat(trend).hasSize(1);
        assertThat(trend.get(0).score()).isEqualTo(45);
        assertThat(trend.get(0).percent()).isEqualTo(90); // 45/50
    }

    @Test
    void 만점_초과_점수는_거부된다() {
        TenantContext.set(TENANT_A);
        AppUser student = account("stu", "STUDENT");
        UUID exam = examService.create(new ExamRequest("단원평가", "과학", LocalDate.of(2026, 5, 1), 100, null)).getId();
        assertThatThrownBy(() ->
                examService.recordScores(exam, List.of(new ScoreEntry(student.getEmail(), 150, null))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 빈_학생이메일_성적은_거부된다() {
        TenantContext.set(TENANT_A);
        UUID exam = examService.create(new ExamRequest("쪽지", "국어", LocalDate.of(2026, 5, 3), 100, null)).getId();
        assertThatThrownBy(() -> examService.recordScores(exam, List.of(new ScoreEntry("  ", 50, null))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 석차와_백분위를_계산한다() {
        TenantContext.set(TENANT_A);
        long n = System.nanoTime();
        String a = "ra" + n + "@acme", b = "rb" + n + "@acme", c = "rc" + n + "@acme";
        UUID exam = examService.create(new ExamRequest("기말고사", "수학", LocalDate.of(2026, 7, 1), 100, null)).getId();
        examService.recordScores(exam, List.of(
                new ScoreEntry(a, 95, null), new ScoreEntry(b, 80, null), new ScoreEntry(c, 60, null)));

        var top = examService.studentScores(a).get(0);
        assertThat(top.rank()).isEqualTo(1);
        assertThat(top.totalTakers()).isEqualTo(3);
        assertThat(top.topPercent()).isEqualTo(34);   // ceil(1/3*100)
        assertThat(examService.studentScores(b).get(0).rank()).isEqualTo(2);

        var ranking = examService.ranking(exam);
        assertThat(ranking.get(0).studentSubject()).isEqualTo(a);
        assertThat(ranking.get(0).rank()).isEqualTo(1);
        assertThat(ranking.get(2).rank()).isEqualTo(3);
    }

    @Test
    void 리포트는_성적과_수강을_집계한다() {
        TenantContext.set(TENANT_A);
        AppUser student = account("stu", "STUDENT");
        UUID courseId = courseService.findAll().get(0).getId();
        enrollmentService.enroll(courseId, student.getEmail());
        UUID exam = examService.create(new ExamRequest("진단평가", "수학", LocalDate.of(2026, 3, 2), 100, null)).getId();
        examService.recordScores(exam, List.of(new ScoreEntry(student.getEmail(), 88, null)));

        AppUser parent = account("par", "PARENT");
        guardianService.link(parent.getEmail(), student.getEmail());
        StudentReport report = guardianService.childReport(parent.getEmail(), student.getEmail());

        assertThat(report.studentName()).isEqualTo(student.getDisplayName());
        assertThat(report.scores()).hasSize(1);
        assertThat(report.scoreAvgPercent()).isEqualTo(88);
        assertThat(report.latestPercent()).isEqualTo(88);
        assertThat(report.courses().enrolled()).isEqualTo(1);
    }

    @Test
    void 연결되지_않은_자녀_리포트는_403() {
        TenantContext.set(TENANT_A);
        AppUser parent = account("par", "PARENT");
        AppUser student = account("stu", "STUDENT");
        // 연결하지 않음
        assertThatThrownBy(() -> guardianService.childReport(parent.getEmail(), student.getEmail()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> guardianService.childScores(parent.getEmail(), student.getEmail()))
                .isInstanceOf(ForbiddenException.class);
    }
}
