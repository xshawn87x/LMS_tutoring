package com.lms.tier3;

import com.lms.course.CourseService;
import com.lms.course.dto.CourseRequest;
import com.lms.error.BadRequestException;
import com.lms.market.MarketService;
import com.lms.tenant.TenantContext;
import com.lms.tuition.StudentPaymentService;
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

/** Tier3: 수강료 결제/환불 + 콘텐츠 마켓 등록·구매·정산. */
@SpringBootTest
@Testcontainers
class Tier3Test {

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

    @Autowired CourseService courseService;
    @Autowired StudentPaymentService paymentService;
    @Autowired MarketService marketService;

    @Test
    void 수강료_설정_결제_환불() {
        TenantContext.set(TENANT_A);
        var c = courseService.create(new CourseRequest("유료 강의", "x", null, 1));
        courseService.setTuition(c.getId(), 50000);

        var pay = paymentService.pay("stu@acme", c.getId());
        assertThat(pay.getAmount()).isEqualTo(50000);
        assertThat(pay.getStatus()).isEqualTo("PAID");
        assertThat(paymentService.listMine("stu@acme")).anyMatch(p -> p.getId().equals(pay.getId()));

        var refunded = paymentService.refund(pay.getId());
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void 무료강의는_결제할_수_없다() {
        TenantContext.set(TENANT_A);
        var free = courseService.create(new CourseRequest("무료 강의", "x", null, 0));
        assertThatThrownBy(() -> paymentService.pay("stu@acme", free.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void 마켓_콘텐츠_등록_구매_정산() {
        TenantContext.set(TENANT_A);
        var content = marketService.create("인기 수학 강의팩", "고1 수학", "MATH", 300000, "본사콘텐츠");
        assertThat(marketService.listPublished()).anyMatch(x -> x.getId().equals(content.getId()));

        var purchase = marketService.purchase(content.getId(), "admin@acme");
        assertThat(purchase.getAmount()).isEqualTo(300000);
        assertThat(marketService.myPurchases()).anyMatch(p -> p.getContentId().equals(content.getId()));

        // 중복 구매 방지
        assertThatThrownBy(() -> marketService.purchase(content.getId(), "admin@acme"))
                .isInstanceOf(RuntimeException.class);

        // 정산 집계
        var settlements = marketService.settlements();
        assertThat(settlements).anyMatch(s -> s.contentId().equals(content.getId()) && s.revenue() >= 300000);
    }
}
