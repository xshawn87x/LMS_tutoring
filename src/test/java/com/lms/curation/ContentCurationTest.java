package com.lms.curation;

import com.lms.course.CourseService;
import com.lms.feature.Feature;
import com.lms.feature.FeatureDisabledException;
import com.lms.feature.FeatureService;
import com.lms.feature.TenantFeatureRepository;
import com.lms.learner.LearnerProfileService;
import com.lms.learner.RecommendationService;
import com.lms.learner.dto.LearnerDtos.ProfileRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 콘텐츠 분석(휴리스틱) + AI_CURATION 게이팅 + 테넌트 격리 검증. */
@SpringBootTest
@Testcontainers
class ContentCurationTest {

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

    @Autowired ContentInsightService insightService;
    @Autowired FeatureService featureService;
    @Autowired CourseService courseService;
    @Autowired TenantFeatureRepository tenantFeatureRepository;
    @Autowired LearnerProfileService profileService;
    @Autowired RecommendationService recommendationService;

    @AfterEach
    void reset() {
        for (UUID tenant : new UUID[]{TENANT_A, TENANT_B}) {
            TenantContext.set(tenant);
            tenantFeatureRepository.deleteAll();
        }
        TenantContext.clear();
    }

    private UUID enableAndGetCourse() {
        TenantContext.set(TENANT_A);
        featureService.setEnabled(Feature.AI_CURATION, true);
        return courseService.findAll().get(0).getId(); // 시드: Spring 입문 (category BACKEND)
    }

    @Test
    void 휴리스틱_분석은_태그와_요약을_만든다() {
        UUID courseId = enableAndGetCourse();
        ContentInsight insight = insightService.analyze(courseId);

        assertThat(insight.getGeneratedBy()).isEqualTo("HEURISTIC");
        assertThat(insight.getTags()).contains("backend"); // 분야 코드가 태그로
        assertThat(insight.getTags()).contains("spring");   // 제목 'Spring 입문'에서
        assertThat(insight.getSummary()).isNotBlank();
        assertThat(insight.getDifficulty()).isNotNull();
    }

    @Test
    void AI_CURATION이_꺼져있으면_분석이_차단된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();
        // 기본값 OFF
        assertThatThrownBy(() -> insightService.analyze(courseId))
                .isInstanceOf(FeatureDisabledException.class);
    }

    @Test
    void 분석결과는_테넌트별로_격리된다() {
        UUID courseId = enableAndGetCourse();
        insightService.analyze(courseId);
        assertThat(insightService.get(courseId)).isPresent();

        // B에서 AI_CURATION을 켜도 A의 인사이트는 RLS로 보이지 않음
        TenantContext.set(TENANT_B);
        featureService.setEnabled(Feature.AI_CURATION, true);
        assertThat(insightService.get(courseId)).isEmpty();
    }

    @Test
    void 분석_태그가_추천_점수에_반영된다() {
        UUID courseId = enableAndGetCourse();   // AI_CURATION on, 'Spring 입문'(BACKEND)
        insightService.analyze(courseId);        // 태그에 backend/spring 포함

        // 백엔드 관심 학습자
        profileService.saveProfile("tag-user",
                new ProfileRequest(java.util.List.of("BACKEND"), java.util.List.of(new SkillView("BACKEND", 0))));

        RecommendationResponse rec = recommendationService.recommend("tag-user").stream()
                .filter(r -> r.courseId().equals(courseId))
                .findFirst().orElseThrow();
        assertThat(rec.reason()).contains("콘텐츠 태그 일치");
    }
}
