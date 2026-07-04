package com.lms.tenant;

import com.lms.course.Course;
import com.lms.course.CourseService;
import com.lms.course.dto.CourseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RLS 행 단위 테넌트 격리 검증 — 이 슬라이스의 성공 기준.
 *
 * 코드에 WHERE tenant_id 가 한 줄도 없는데도, TenantContext에 어떤 테넌트를
 * 올려두느냐에 따라 보이는 행이 완전히 달라진다 → 격리는 전적으로 DB(RLS)가 강제한다.
 */
@SpringBootTest
@Testcontainers
class TenantIsolationTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lms")
            .withUsername("lms_owner")
            .withPassword("lms_owner_pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // 앱 런타임은 RLS 대상인 lms_app 롤로 접속 (V1 마이그레이션이 생성)
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lms_app");
        registry.add("spring.datasource.password", () -> "lms_app_pw");
        // Flyway는 소유자(컨테이너 기본 계정 = lms_owner)로 마이그레이션 실행
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    CourseService courseService;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void 테넌트A는_자신의_과정만_본다() {
        TenantContext.set(TENANT_A);
        List<Course> courses = courseService.findAll();

        // V12 시드 카탈로그: A에 5개 과정
        assertThat(courses).hasSize(5);
        assertThat(courses).allMatch(c -> c.getTenantId().equals(TENANT_A));
        assertThat(courses).extracting(Course::getTitle)
                .containsExactlyInAnyOrder("Spring 입문", "JPA 기초",
                        "React로 시작하는 프론트엔드", "실전 SQL과 데이터 모델링", "Docker로 배우는 컨테이너 기초");
    }

    @Test
    void 테넌트B는_자신의_과정만_본다() {
        TenantContext.set(TENANT_B);
        List<Course> courses = courseService.findAll();

        // V12 시드 카탈로그: B에 2개 과정
        assertThat(courses).hasSize(2);
        assertThat(courses).allMatch(c -> c.getTenantId().equals(TENANT_B));
        assertThat(courses).extracting(Course::getTitle)
                .containsExactlyInAnyOrder("Python 입문", "Pandas 데이터 분석 입문");
    }

    @Test
    void 테넌트A가_생성한_과정은_테넌트B에게_보이지_않는다() {
        TenantContext.set(TENANT_A);
        Course created = courseService.create(new CourseRequest("A 전용 비밀 과정", "leak 되면 안 됨", null, null));
        UUID id = created.getId();

        // 같은 테넌트(A)는 방금 만든 과정을 조회할 수 있다
        TenantContext.set(TENANT_A);
        assertThat(courseService.findById(id)).isPresent();

        // 다른 테넌트(B)에게는 RLS가 행을 숨긴다 → 조회 불가
        TenantContext.set(TENANT_B);
        assertThat(courseService.findById(id)).isEmpty();
    }

    @Test
    void 테넌트_컨텍스트가_없으면_아무것도_보이지_않는다_failClosed() {
        // TenantContext 미설정 → sentinel(0 UUID) → 어떤 행도 매칭 안 됨
        List<Course> courses = courseService.findAll();
        assertThat(courses).isEmpty();
    }
}
