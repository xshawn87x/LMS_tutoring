package com.lms.billing;

import com.lms.feature.Feature;
import com.lms.platform.EntitlementService;
import com.lms.platform.Plan;
import com.lms.platform.TenantEntitlementRepository;
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

/** 가격 카탈로그 + 청구 명세(플랜+애드온+사용량) + 인보이스 발행/결제. */
@SpringBootTest
@Testcontainers
class BillingTest {

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

    @Autowired BillingService billingService;
    @Autowired EntitlementService entitlementService;
    @Autowired UsageService usageService;
    @Autowired TenantEntitlementRepository entitlementRepository;
    @Autowired UsageCounterRepository usageRepository;
    @Autowired InvoiceRepository invoiceRepository;

    @AfterEach
    void reset() {
        TenantContext.set(TENANT_A);
        invoiceRepository.findByTenantIdOrderByIssuedAtDesc(TENANT_A).forEach(invoiceRepository::delete);
        usageRepository.findByTenantId(TENANT_A).forEach(usageRepository::delete);
        entitlementRepository.findByTenantId(TENANT_A).forEach(entitlementRepository::delete);
        entitlementService.applyPlan(TENANT_A, Plan.PRO);   // 시드 상태 복원
        TenantContext.clear();
    }

    @Test
    void 가격_카탈로그가_로드된다() {
        assertThat(billingService.planPrices()).extracting(p -> p.getPlan().name())
                .contains("FREE", "STANDARD", "PRO");
        assertThat(billingService.addonPrices()).extracting(p -> p.getFeature().name())
                .contains("CERTIFICATES", "AI_CURATION");
    }

    @Test
    void PRO_기본_명세는_요금제가_플러스_부가세만_청구된다() {
        TenantContext.set(TENANT_A);
        var s = billingService.currentStatement(TENANT_A);
        assertThat(s.lines()).anyMatch(l -> l.kind().equals("PLAN") && l.amount() == 99000);
        assertThat(s.lines()).anyMatch(l -> l.kind().equals("TAX") && l.amount() == 9900);   // 부가세 10%
        assertThat(s.total()).isEqualTo(108900);                // 99,000 + 부가세 9,900
        assertThat(s.lines()).filteredOn(l -> l.kind().equals("USAGE")).allMatch(l -> l.amount() == 0);
    }

    @Test
    void 정액_애드온은_명세에_가산된다() {
        TenantContext.set(TENANT_A);
        // STANDARD엔 수료증 미포함 → 애드온으로 부여하면 정액 20,000 가산
        entitlementService.applyPlan(TENANT_A, Plan.STANDARD);
        entitlementService.grantAddon(TENANT_A, Feature.CERTIFICATES);

        var s = billingService.currentStatement(TENANT_A);
        assertThat(s.lines()).anyMatch(l -> l.kind().equals("ADDON") && l.amount() == 20000);
        assertThat(s.total()).isEqualTo((49000 + 20000) * 11 / 10);   // (STANDARD + 수료증) + 부가세 = 75,900
    }

    @Test
    void 사용량은_무료_포함량_초과분만_과금된다() {
        TenantContext.set(TENANT_A);   // PRO → AI_CURATION 자격 있음
        for (int i = 0; i < 52; i++) {
            usageService.record(Feature.AI_CURATION);
        }
        var s = billingService.currentStatement(TENANT_A);
        // 52회 사용, 50 무료 포함 → 2 × 500 = 1000원
        assertThat(s.lines()).anyMatch(l -> l.kind().equals("USAGE") && l.amount() == 1000);
        assertThat(s.total()).isEqualTo((99000 + 1000) * 11 / 10);    // 100,000 + 부가세 = 110,000
    }

    @Test
    void 인보이스_발행_후_결제하면_PAID가_된다() {
        TenantContext.set(TENANT_A);
        String period = BillingPeriods.current();

        var invoice = billingService.issueInvoice(TENANT_A, period);
        assertThat(invoice.getStatus()).isEqualTo("ISSUED");
        assertThat(invoice.getTotal()).isEqualTo(108900);       // 부가세 포함

        var paid = billingService.payInvoice(invoice.getId());
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getPaymentRef()).startsWith("mock_");
        assertThat(paid.getPaidAt()).isNotNull();
    }

    @Test
    void 발행은_같은_달_미결제_인보이스를_최신_명세로_교체한다() {
        TenantContext.set(TENANT_A);
        String period = BillingPeriods.current();

        var first = billingService.issueInvoice(TENANT_A, period);
        // 사용량이 늘어난 뒤 재발행 → 같은 (tenant, period)라 교체되고 총액이 갱신됨
        for (int i = 0; i < 60; i++) usageService.record(Feature.AI_CURATION);
        var second = billingService.issueInvoice(TENANT_A, period);

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(invoiceRepository.findByTenantIdOrderByIssuedAtDesc(TENANT_A)).hasSize(1);
        assertThat(second.getTotal()).isEqualTo((99000 + 10 * 500) * 11 / 10);  // (60-50=10 초과) + 부가세
    }

    @Test
    void 요금제_가격을_수정할_수_있다() {
        billingService.updatePlanPrice(Plan.PRO, 120000);
        TenantContext.set(TENANT_A);
        var s = billingService.currentStatement(TENANT_A);
        assertThat(s.lines()).anyMatch(l -> l.kind().equals("PLAN") && l.amount() == 120000);
        assertThat(s.total()).isEqualTo(132000);                // 120,000 + 부가세
        billingService.updatePlanPrice(Plan.PRO, 99000);        // 원복
    }
}
