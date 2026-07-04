package com.lms.feature;

import com.lms.course.CourseService;
import com.lms.quiz.QuizService;
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

/** 테넌트별 기능 플래그: 기본값, 토글, 비활성 시 모듈 차단, 테넌트 격리. */
@SpringBootTest
@Testcontainers
class FeatureFlagTest {

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

    @Autowired FeatureService featureService;
    @Autowired QuizService quizService;
    @Autowired CourseService courseService;
    @Autowired TenantFeatureRepository tenantFeatureRepository;

    @AfterEach
    void clear() {
        // 같은 컨테이너를 공유하므로 테스트 간 override를 초기화해 순서 독립성을 보장
        for (UUID tenant : new UUID[]{TENANT_A, TENANT_B}) {
            TenantContext.set(tenant);
            tenantFeatureRepository.deleteAll();
        }
        TenantContext.clear();
    }

    @Test
    void 기본값은_enum_default를_따른다() {
        TenantContext.set(TENANT_A);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isTrue();        // default ON
        assertThat(featureService.isEnabled(Feature.CERTIFICATES)).isFalse();  // default OFF
    }

    @Test
    void 퀴즈_기능을_끄면_퀴즈_모듈이_차단된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();

        // 켜져 있을 때는 생성 가능
        var quiz = quizService.createQuiz(courseId, "before");
        assertThat(quiz).isNotNull();

        // 끄면 차단 (403)
        featureService.setEnabled(Feature.QUIZZES, false);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isFalse();
        assertThatThrownBy(() -> quizService.createQuiz(courseId, "after"))
                .isInstanceOf(FeatureDisabledException.class);
        assertThatThrownBy(() -> quizService.listQuizzes(courseId))
                .isInstanceOf(FeatureDisabledException.class);
    }

    @Test
    void 플래그는_테넌트별로_독립이다() {
        // A에서 퀴즈 끄기
        TenantContext.set(TENANT_A);
        featureService.setEnabled(Feature.QUIZZES, false);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isFalse();

        // B는 영향 없음 (RLS로 A의 override 행이 안 보임 → 기본값 ON)
        TenantContext.set(TENANT_B);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isTrue();
    }
}
