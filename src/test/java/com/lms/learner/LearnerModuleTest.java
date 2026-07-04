package com.lms.learner;

import com.lms.course.CourseService;
import com.lms.enrollment.EnrollmentService;
import com.lms.learner.dto.LearnerDtos.ProfileRequest;
import com.lms.learner.dto.LearnerDtos.ProfileResponse;
import com.lms.learner.dto.LearnerDtos.RecommendationResponse;
import com.lms.learner.dto.LearnerDtos.SkillView;
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

/** 학습자 프로필(자가 진단) + 규칙 기반 추천 + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class LearnerModuleTest {

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

    @Autowired LearnerProfileService profileService;
    @Autowired RecommendationService recommendationService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired CourseService courseService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void 프로필_저장_후_조회되고_온보딩_처리된다() {
        TenantContext.set(TENANT_A);
        ProfileResponse saved = profileService.saveProfile("alice",
                new ProfileRequest(List.of("BACKEND", "DATA"), List.of(new SkillView("BACKEND", 0))));

        assertThat(saved.onboarded()).isTrue();
        assertThat(saved.interests()).containsExactlyInAnyOrder("BACKEND", "DATA");
        assertThat(saved.skills()).extracting(SkillView::categoryCode).containsExactly("BACKEND");
    }

    @Test
    void 관심분야_백엔드_입문자는_백엔드_과정이_먼저_추천된다() {
        TenantContext.set(TENANT_A);
        // 백엔드 관심 + 입문 수준
        profileService.saveProfile("bob",
                new ProfileRequest(List.of("BACKEND"), List.of(new SkillView("BACKEND", 0))));

        List<RecommendationResponse> recs = recommendationService.recommend("bob");
        assertThat(recs).isNotEmpty();
        // 최상위 추천은 백엔드 과정이어야 한다 (시드: Spring 입문/JPA 기초 = BACKEND)
        assertThat(recs.get(0).categoryCode()).isEqualTo("BACKEND");
        assertThat(recs.get(0).reason()).contains("관심분야");
    }

    @Test
    void 인기_과정은_추천_이유에_표시된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        enrollmentService.enroll(courseId, "pop1");
        enrollmentService.enroll(courseId, "pop2");
        enrollmentService.enroll(courseId, "pop3");

        // 관심분야 없는 신규 학습자 — 행동 신호(인기도)만으로도 추천 이유가 붙는다
        RecommendationResponse rec = recommendationService.recommend("pop-viewer").stream()
                .filter(r -> r.courseId().equals(courseId)).findFirst().orElseThrow();
        assertThat(rec.reason()).contains("인기 과정");
    }

    @Test
    void 이미_수강한_과정은_추천에서_제외된다() {
        TenantContext.set(TENANT_A);
        profileService.saveProfile("carol",
                new ProfileRequest(List.of("BACKEND"), List.of(new SkillView("BACKEND", 0))));
        List<RecommendationResponse> before = recommendationService.recommend("carol");
        UUID topCourse = before.get(0).courseId();

        enrollmentService.enroll(topCourse, "carol");

        List<RecommendationResponse> after = recommendationService.recommend("carol");
        assertThat(after).extracting(RecommendationResponse::courseId).doesNotContain(topCourse);
    }

    @Test
    void 프로필을_두번_저장해도_충돌없이_교체된다() {
        TenantContext.set(TENANT_A);
        profileService.saveProfile("repeat",
                new ProfileRequest(List.of("BACKEND", "DATA"), List.of(new SkillView("BACKEND", 1))));

        // 두 번째 저장 — 겹치는 관심분야 포함 (수정 전엔 uq_learner_interest 위반으로 500)
        ProfileResponse r = profileService.saveProfile("repeat",
                new ProfileRequest(List.of("BACKEND", "FRONTEND"), List.of(new SkillView("BACKEND", 2))));

        assertThat(r.interests()).containsExactlyInAnyOrder("BACKEND", "FRONTEND");
        assertThat(r.skills()).extracting(SkillView::categoryCode).containsExactly("BACKEND");
        assertThat(r.skills().get(0).level()).isEqualTo(2);
    }

    @Test
    void 프로필은_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        profileService.saveProfile("dave", new ProfileRequest(List.of("BACKEND"), List.of()));

        // 같은 student_id라도 B 테넌트에는 프로필이 없다 (RLS)
        TenantContext.set(TENANT_B);
        ProfileResponse bView = profileService.getProfile("dave");
        assertThat(bView.onboarded()).isFalse();
        assertThat(bView.interests()).isEmpty();
    }
}
