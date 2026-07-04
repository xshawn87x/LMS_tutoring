package com.lms.auth;

import com.lms.auth.dto.AuthDtos.LoginRequest;
import com.lms.auth.dto.AuthDtos.PasswordResetConfirm;
import com.lms.auth.dto.AuthDtos.RegisterRequest;
import com.lms.error.UnauthorizedException;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 비밀번호 재설정: 토큰 발급 → 확정 → 새 비번 로그인. */
@SpringBootTest
@Testcontainers
class PasswordResetTest {

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

    @Autowired AuthService authService;

    @Test
    void 토큰_발급_후_새_비밀번호로_로그인된다() {
        Tenant acme = authService.resolveTenant("acme");
        TenantContext.set(acme.getId());
        String email = "reset_" + System.nanoTime() + "@e.com";
        authService.register(acme, new RegisterRequest("acme", email, "oldpass123", "리셋", "STUDENT"));

        // 재설정 토큰 발급
        String token = authService.requestPasswordReset(email);
        assertThat(token).isNotBlank();

        // 새 비번 확정
        authService.confirmPasswordReset(new PasswordResetConfirm("acme", email, token, "newpass456"));

        // 새 비번 로그인 성공, 옛 비번 실패
        assertThat(authService.login(acme, new LoginRequest("acme", email, "newpass456"))).isNotNull();
        assertThatThrownBy(() -> authService.login(acme, new LoginRequest("acme", email, "oldpass123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 사용된_토큰은_재사용_불가() {
        Tenant acme = authService.resolveTenant("acme");
        TenantContext.set(acme.getId());
        String email = "reset2_" + System.nanoTime() + "@e.com";
        authService.register(acme, new RegisterRequest("acme", email, "oldpass123", "리셋2", "STUDENT"));

        String token = authService.requestPasswordReset(email);
        authService.confirmPasswordReset(new PasswordResetConfirm("acme", email, token, "newpass456"));
        // 같은 토큰 재사용 → 실패
        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirm("acme", email, token, "another789")))
                .isInstanceOf(RuntimeException.class);
    }
}
