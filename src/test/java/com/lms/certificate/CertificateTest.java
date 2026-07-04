package com.lms.certificate;

import com.lms.certificate.dto.CertificateResponse;
import com.lms.course.CourseService;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.feature.TenantFeatureRepository;
import com.lms.quiz.Quiz;
import com.lms.quiz.QuizService;
import com.lms.quiz.dto.QuizDtos.QuestionRequest;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 수료(과정 완료) 판정 + 수료증 발급 + 게이팅 + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class CertificateTest {

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

    @Autowired CertificateService certificateService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired QuizService quizService;
    @Autowired CourseService courseService;
    @Autowired FeatureService featureService;
    @Autowired TenantFeatureRepository tenantFeatureRepository;

    @AfterEach
    void reset() {
        for (UUID t : new UUID[]{TENANT_A, TENANT_B}) {
            TenantContext.set(t);
            tenantFeatureRepository.deleteAll();
        }
        TenantContext.clear();
    }

    private UUID noQuizCourse() {  // 시드 'Spring 입문' — 퀴즈 없음
        TenantContext.set(TENANT_A);
        featureService.setEnabled(Feature.CERTIFICATES, true);
        return courseService.findAll().get(0).getId();
    }

    @Test
    void 퀴즈없는_과정은_진도100이면_수료증_발급() {
        UUID courseId = noQuizCourse();
        Enrollment e = enrollmentService.enroll(courseId, "alice");
        enrollmentService.updateProgress(e.getId(), "alice", 100);

        CertificateResponse cert = certificateService.getForCourse(courseId, "alice");
        assertThat(cert).isNotNull();
        assertThat(cert.certificateNo()).startsWith("CERT-");
    }

    @Test
    void 진도가_미달이면_수료증_없음() {
        UUID courseId = noQuizCourse();
        Enrollment e = enrollmentService.enroll(courseId, "bob");
        enrollmentService.updateProgress(e.getId(), "bob", 50);

        assertThat(certificateService.getForCourse(courseId, "bob")).isNull();
    }

    @Test
    void 퀴즈가_있으면_모두_통과해야_수료증() {
        TenantContext.set(TENANT_A);
        featureService.setEnabled(Feature.CERTIFICATES, true);
        UUID courseId = courseService.findAll().get(1).getId(); // 'JPA 기초' — 여기에만 퀴즈 추가
        Quiz quiz = quizService.createQuiz(courseId, "쪽지시험");
        quizService.addQuestion(quiz.getId(), new QuestionRequest("1+1?", List.of("2", "3"), 0, 1));

        Enrollment e = enrollmentService.enroll(courseId, "carol");
        // 진도 100이지만 퀴즈 미응시 → 미발급
        enrollmentService.updateProgress(e.getId(), "carol", 100);
        assertThat(certificateService.getForCourse(courseId, "carol")).isNull();

        // 퀴즈 통과(정답) 후 다시 진도 갱신 → 발급
        quizService.submit(quiz.getId(), "carol", List.of(0));
        enrollmentService.updateProgress(e.getId(), "carol", 100);
        assertThat(certificateService.getForCourse(courseId, "carol")).isNotNull();
    }

    @Test
    void CERTIFICATES_꺼지면_발급되지_않는다() {
        TenantContext.set(TENANT_A);
        // 기능 OFF 상태(기본). 진도 100이어도 발급 안 됨.
        UUID courseId = courseService.findAll().get(0).getId();
        Enrollment e = enrollmentService.enroll(courseId, "dave");
        enrollmentService.updateProgress(e.getId(), "dave", 100);

        // 확인을 위해 켜고 조회 → 없음
        featureService.setEnabled(Feature.CERTIFICATES, true);
        assertThat(certificateService.getForCourse(courseId, "dave")).isNull();
    }

    @Test
    void 수료증은_테넌트별로_격리된다() {
        UUID courseId = noQuizCourse();
        Enrollment e = enrollmentService.enroll(courseId, "erin");
        enrollmentService.updateProgress(e.getId(), "erin", 100);
        assertThat(certificateService.getForCourse(courseId, "erin")).isNotNull();

        // B에서 CERTIFICATES 켜도 A의 수료증은 RLS로 안 보임
        TenantContext.set(TENANT_B);
        featureService.setEnabled(Feature.CERTIFICATES, true);
        assertThat(certificateService.myCertificates("erin")).isEmpty();
    }
}
