package com.lms.platform;

import com.lms.billing.BillingService;
import com.lms.billing.Invoice;
import com.lms.billing.InvoiceRepository;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
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

import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 기관 정지(수동)·연체(자동) 게이팅 + 청구 마감. */
@SpringBootTest
@Testcontainers
class PlatformOpsTest {

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

    @Autowired AccountStandingService standingService;
    @Autowired BillingService billingService;
    @Autowired FeatureService featureService;
    @Autowired InvoiceRepository invoiceRepository;

    @AfterEach
    void reset() {
        for (UUID t : new UUID[]{TENANT_A, TENANT_B}) {
            invoiceRepository.findByTenantIdOrderByIssuedAtDesc(t).forEach(invoiceRepository::delete);
            standingService.reactivate(t);   // 상태 ACTIVE로 복원
        }
    }

    @Test
    void 수동_정지된_기관은_모듈이_차단된다() {
        standingService.suspend(TENANT_A);
        TenantContext.set(TENANT_A);
        assertThatThrownBy(() -> featureService.requireEnabled(Feature.QUIZZES))
                .isInstanceOf(AccountSuspendedException.class);

        standingService.reactivate(TENANT_A);
        assertThatCode(() -> featureService.requireEnabled(Feature.QUIZZES)).doesNotThrowAnyException();
    }

    @Test
    void 지난달_미결제면_연체로_차단되고_결제하면_해제된다() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();

        // 지난달 청구 마감 → 미결제 인보이스 발행 → 연체(PAST_DUE)
        billingService.closePeriod(lastMonth);
        TenantContext.set(TENANT_A);
        assertThatThrownBy(() -> featureService.requireEnabled(Feature.QUIZZES))
                .isInstanceOf(AccountSuspendedException.class);

        // 지난달 인보이스 결제 → 연체 해소 → 이용 재개
        Invoice past = invoiceRepository.findByTenantIdAndPeriod(TENANT_A, lastMonth).orElseThrow();
        billingService.payInvoice(past.getId());
        assertThatCode(() -> featureService.requireEnabled(Feature.QUIZZES)).doesNotThrowAnyException();
    }

    @Test
    void 청구_마감은_모든_기관에_인보이스를_발행한다() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        var issued = billingService.closePeriod(lastMonth);
        assertThat(issued).hasSizeGreaterThanOrEqualTo(2);   // A, B 최소 2곳
        assertThat(invoiceRepository.findByTenantIdAndPeriod(TENANT_A, lastMonth)).isPresent();
        assertThat(invoiceRepository.findByTenantIdAndPeriod(TENANT_B, lastMonth)).isPresent();
    }
}
