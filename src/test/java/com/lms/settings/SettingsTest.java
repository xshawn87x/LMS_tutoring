package com.lms.settings;

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

/** 학원 환경설정: 테넌트당 1행 upsert + RLS 격리. */
@SpringBootTest
@Testcontainers
class SettingsTest {

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

    @Autowired TenantSettingsRepository repository;

    @Test
    void 환경설정_저장하면_같은_테넌트만_읽는다() {
        TenantContext.set(TENANT_A);
        TenantSettings s = new TenantSettings(TENANT_A);
        s.apply("Acme 러닝센터", "/media/logo.png", "#ff0000", "02-000-0000", "약관");
        repository.save(s);

        // 같은 테넌트: RLS로 자기 행만 보임
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getDisplayName()).isEqualTo("Acme 러닝센터");

        // 다른 테넌트: A의 설정이 안 보임
        TenantContext.set(TENANT_B);
        assertThat(repository.findAll()).noneMatch(x -> "Acme 러닝센터".equals(x.getDisplayName()));
    }

    @Test
    void 같은_테넌트_재저장은_1행을_유지한다() {
        TenantContext.set(TENANT_A);
        TenantSettings first = repository.findById(TENANT_A).orElseGet(() -> new TenantSettings(TENANT_A));
        first.apply("이름1", null, null, null, null);
        repository.save(first);

        TenantSettings again = repository.findById(TENANT_A).orElseThrow();
        again.apply("이름2", null, "#000000", null, null);
        repository.save(again);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(TENANT_A).orElseThrow().getDisplayName()).isEqualTo("이름2");
    }
}
