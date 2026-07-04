package com.lms.admin;

import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 학원 관리자 회원/강사 관리: 생성·역할변경·비번리셋·삭제·테넌트 격리. */
@SpringBootTest
@Testcontainers
class MemberManagementTest {

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

    @Autowired MemberService memberService;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void 강사_생성_역할변경_비번리셋_삭제() {
        TenantContext.set(TENANT_A);
        String email = "teacher_" + System.nanoTime() + "@e.com";
        AppUser created = memberService.create(email, "initpass1", "김강사", List.of("INSTRUCTOR"));
        assertThat(memberService.list()).anyMatch(u -> u.getId().equals(created.getId()));

        memberService.updateRoles(created.getId(), List.of("INSTRUCTOR", "ADMIN"));
        assertThat(userRepository.findById(created.getId()).orElseThrow().roleList())
                .containsExactlyInAnyOrder("INSTRUCTOR", "ADMIN");

        memberService.resetPassword(created.getId(), "resetpass9");
        assertThat(passwordEncoder.matches("resetpass9",
                userRepository.findById(created.getId()).orElseThrow().getPasswordHash())).isTrue();

        memberService.delete(created.getId());
        assertThat(userRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void 회원은_테넌트별로_격리된다() {
        TenantContext.set(TENANT_A);
        String email = "amember_" + System.nanoTime() + "@e.com";
        memberService.create(email, "initpass1", "A회원", List.of("STUDENT"));

        TenantContext.set(TENANT_B);
        assertThat(memberService.list()).noneMatch(u -> u.getEmail().equals(email));
    }
}
