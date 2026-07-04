package com.lms.platform;

import com.lms.course.CourseService;
import com.lms.feature.Feature;
import com.lms.feature.FeatureDisabledException;
import com.lms.feature.FeatureService;
import com.lms.feature.TenantFeatureRepository;
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

/**
 * 2계층(자격+활성화) 판정: 요금제 자격이 없으면 켤 수 없고 모듈이 차단된다.
 * 유효 활성 = 자격 있음 AND 기관이 켬.
 */
@SpringBootTest
@Testcontainers
class EntitlementTest {

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

    @Autowired EntitlementService entitlementService;
    @Autowired FeatureService featureService;
    @Autowired QuizService quizService;
    @Autowired CourseService courseService;
    @Autowired TenantEntitlementRepository entitlementRepository;
    @Autowired TenantFeatureRepository tenantFeatureRepository;

    @AfterEach
    void reset() {
        // 공유 컨테이너 → 각 테넌트를 시드 상태(PRO 전체 자격, override 없음)로 되돌려 순서 독립성 보장
        for (UUID tenant : new UUID[]{TENANT_A, TENANT_B}) {
            TenantContext.set(tenant);
            tenantFeatureRepository.deleteAll();                 // RLS로 현재 테넌트 override만 삭제
            entitlementRepository.findByTenantId(tenant)         // RLS-free → 반드시 tenant로 스코프
                    .forEach(entitlementRepository::delete);
            entitlementService.applyPlan(tenant, Plan.PRO);
        }
        TenantContext.clear();
    }

    @Test
    void 자격이_없으면_기본값이_ON이어도_비활성이다() {
        TenantContext.set(TENANT_A);
        // FREE = 레슨/수강만. 퀴즈는 자격 밖.
        entitlementService.applyPlan(TENANT_A, Plan.FREE);

        assertThat(featureService.isEnabled(Feature.LESSONS)).isTrue();     // FREE에 포함 + 기본 ON
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isFalse();    // 자격 없음 → 기본 ON이어도 OFF
    }

    @Test
    void 자격없는_기능은_기관이_켤_수_없다() {
        TenantContext.set(TENANT_A);
        entitlementService.applyPlan(TENANT_A, Plan.FREE);

        assertThatThrownBy(() -> featureService.setEnabled(Feature.QUIZZES, true))
                .isInstanceOf(FeatureNotEntitledException.class);
    }

    @Test
    void 자격을_회수하면_해당_모듈이_차단된다() {
        TenantContext.set(TENANT_A);
        UUID courseId = courseService.findAll().get(0).getId();

        // PRO(시드) 상태 — 퀴즈 생성 가능
        assertThat(quizService.createQuiz(courseId, "before")).isNotNull();

        // 요금제를 FREE로 낮추면 퀴즈 자격이 사라져 차단(403)
        entitlementService.applyPlan(TENANT_A, Plan.FREE);
        assertThatThrownBy(() -> quizService.createQuiz(courseId, "after"))
                .isInstanceOf(FeatureDisabledException.class);
    }

    @Test
    void 애드온으로_요금제_밖_기능을_개별_부여할_수_있다() {
        TenantContext.set(TENANT_A);
        entitlementService.applyPlan(TENANT_A, Plan.FREE);   // 퀴즈 자격 없음
        assertThat(entitlementService.isEntitled(TENANT_A, Feature.QUIZZES)).isFalse();

        // 애드온으로 퀴즈만 개별 부여 → 자격 생김
        entitlementService.grantAddon(TENANT_A, Feature.QUIZZES);
        assertThat(entitlementService.isEntitled(TENANT_A, Feature.QUIZZES)).isTrue();
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isTrue();   // 자격 + 기본 ON
    }

    @Test
    void 요금제_변경은_애드온_자격을_보존한다() {
        TenantContext.set(TENANT_A);
        entitlementService.applyPlan(TENANT_A, Plan.FREE);
        entitlementService.grantAddon(TENANT_A, Feature.AI_CURATION);  // FREE 밖 애드온

        // STANDARD로 변경(AI_CURATION 미포함)해도 애드온 자격은 유지
        entitlementService.applyPlan(TENANT_A, Plan.STANDARD);
        assertThat(entitlementService.isEntitled(TENANT_A, Feature.AI_CURATION)).isTrue();
        assertThat(entitlementService.isEntitled(TENANT_A, Feature.QUIZZES)).isTrue();     // STANDARD 포함
    }

    @Test
    void 요금제가_포함하는_기능은_애드온이었어도_PLAN으로_흡수된다() {
        TenantContext.set(TENANT_A);
        entitlementService.applyPlan(TENANT_A, Plan.FREE);
        entitlementService.grantAddon(TENANT_A, Feature.QUIZZES);   // FREE 밖 → ADDON
        assertThat(sourceOf(Feature.QUIZZES)).isEqualTo(EntitlementSource.ADDON);

        // STANDARD는 QUIZZES를 포함 → 애드온이 요금제 안으로 흡수되어 PLAN이 된다
        entitlementService.applyPlan(TENANT_A, Plan.STANDARD);
        assertThat(sourceOf(Feature.QUIZZES)).isEqualTo(EntitlementSource.PLAN);
    }

    private EntitlementSource sourceOf(Feature feature) {
        return entitlementRepository.findByTenantIdAndFeature(TENANT_A, feature)
                .map(TenantEntitlement::getSource).orElse(null);
    }

    @Test
    void 자격은_테넌트별로_독립이다() {
        // A만 FREE로 낮춤
        TenantContext.set(TENANT_A);
        entitlementService.applyPlan(TENANT_A, Plan.FREE);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isFalse();

        // B는 시드 그대로(PRO) → 영향 없음
        TenantContext.set(TENANT_B);
        assertThat(featureService.isEnabled(Feature.QUIZZES)).isTrue();
    }
}
