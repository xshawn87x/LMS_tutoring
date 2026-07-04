package com.lms.auth;

import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.auth.dto.AuthDtos.LoginRequest;
import com.lms.auth.dto.AuthDtos.RegisterRequest;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.error.UnauthorizedException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 회원가입/로그인: bcrypt 검증, 잘못된 자격증명, org_code별 테넌트 격리. */
@SpringBootTest
@Testcontainers
class AuthModuleTest {

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
    @Autowired AccountService accountService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    /** 컨트롤러 흐름을 모사: org_code로 테넌트 조회 후 컨텍스트 세팅. */
    private Tenant ctx(String orgCode) {
        Tenant t = authService.resolveTenant(orgCode);
        TenantContext.set(t.getId());
        return t;
    }

    @Test
    void 회원가입_후_로그인되고_역할이_담긴다() {
        Tenant t = ctx("acme");
        AuthResponse reg = authService.register(t,
                new RegisterRequest("acme", "Alice@Example.com", "password123", "앨리스", "INSTRUCTOR"));
        assertThat(reg.token()).isNotBlank();
        assertThat(reg.subject()).isEqualTo("alice@example.com"); // 이메일 소문자 정규화
        assertThat(reg.roles()).containsExactly("INSTRUCTOR");

        AuthResponse login = authService.login(ctx("acme"),
                new LoginRequest("acme", "alice@example.com", "password123"));
        assertThat(login.token()).isNotBlank();
        assertThat(login.displayName()).isEqualTo("앨리스");
    }

    @Test
    void 중복_이메일은_409() {
        Tenant t = ctx("acme");
        authService.register(t, new RegisterRequest("acme", "dup@example.com", "password123", null, null));
        assertThatThrownBy(() -> authService.register(ctx("acme"),
                new RegisterRequest("acme", "dup@example.com", "password123", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 잘못된_비밀번호는_401() {
        Tenant t = ctx("acme");
        authService.register(t, new RegisterRequest("acme", "bob@example.com", "password123", null, null));
        assertThatThrownBy(() -> authService.login(ctx("acme"),
                new LoginRequest("acme", "bob@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 없는_사용자_로그인은_401() {
        assertThatThrownBy(() -> authService.login(ctx("acme"),
                new LoginRequest("acme", "ghost@example.com", "password123")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 존재하지_않는_기관코드는_404() {
        assertThatThrownBy(() -> authService.resolveTenant("no-such-org"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 비밀번호를_변경하면_새_비밀번호로_로그인된다() {
        Tenant t = ctx("acme");
        authService.register(t, new RegisterRequest("acme", "pw@example.com", "password123", "비번유저", null));

        accountService.changePassword("pw@example.com", "password123", "newpassword456");

        // 옛 비밀번호는 실패, 새 비밀번호는 성공
        assertThatThrownBy(() -> authService.login(ctx("acme"),
                new LoginRequest("acme", "pw@example.com", "password123")))
                .isInstanceOf(UnauthorizedException.class);
        AuthResponse ok = authService.login(ctx("acme"),
                new LoginRequest("acme", "pw@example.com", "newpassword456"));
        assertThat(ok.token()).isNotBlank();
    }

    @Test
    void 현재_비밀번호가_틀리면_변경실패_401() {
        Tenant t = ctx("acme");
        authService.register(t, new RegisterRequest("acme", "pw2@example.com", "password123", null, null));
        assertThatThrownBy(() -> accountService.changePassword("pw2@example.com", "wrong", "newpassword456"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 표시이름을_수정할수_있다() {
        Tenant t = ctx("acme");
        authService.register(t, new RegisterRequest("acme", "name@example.com", "password123", "옛이름", null));
        var updated = accountService.updateProfile("name@example.com", "새이름");
        assertThat(updated.getDisplayName()).isEqualTo("새이름");
    }

    @Test
    void 같은_이메일이라도_기관별로_독립_계정이다() {
        // acme에 가입
        authService.register(ctx("acme"),
                new RegisterRequest("acme", "same@example.com", "password-acme", "Acme측", null));
        // globex에 동일 이메일로 가입 (다른 비번) — RLS로 격리되어 충돌 없음
        authService.register(ctx("globex"),
                new RegisterRequest("globex", "same@example.com", "password-globex", "Globex측", null));

        // 각자 자기 테넌트 비밀번호로만 로그인된다
        AuthResponse a = authService.login(ctx("acme"),
                new LoginRequest("acme", "same@example.com", "password-acme"));
        assertThat(a.displayName()).isEqualTo("Acme측");

        assertThatThrownBy(() -> authService.login(ctx("globex"),
                new LoginRequest("globex", "same@example.com", "password-acme")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
