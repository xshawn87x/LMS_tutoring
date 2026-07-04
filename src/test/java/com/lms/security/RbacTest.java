package com.lms.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 권한 매트릭스 검증 — 역할에 따라 쓰기 작업이 허용/차단되는지 단언한다.
 * 실제 보안 필터 체인 + @PreAuthorize 를 통과시키되, JWT는 테스트 post-processor로 주입한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RbacTest {

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("lms")
            .withUsername("lms_owner")
            .withPassword("lms_owner_pw");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "lms_app");
        registry.add("spring.datasource.password", () -> "lms_app_pw");
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    /** 테넌트 A + 주어진 역할(들)을 가진 JWT 인증을 흉내낸다. */
    private RequestPostProcessor as(String subject, String... roles) {
        return jwt()
                .jwt(j -> j.subject(subject).claim("tenant_id", TENANT_A))
                .authorities(Arrays.stream(roles)
                        .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }

    private static final String COURSE_BODY = "{\"title\":\"테스트 과정\",\"description\":\"d\"}";

    @Test
    void 학생은_과정을_생성할_수_없다() throws Exception {
        mvc.perform(post("/api/courses").with(as("s", Roles.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON).content(COURSE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void 강사는_과정을_생성할_수_있다() throws Exception {
        mvc.perform(post("/api/courses").with(as("i", Roles.INSTRUCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(COURSE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void 관리자는_과정을_생성할_수_있다() throws Exception {
        mvc.perform(post("/api/courses").with(as("a", Roles.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(COURSE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void 누구나_인증되면_과정을_조회할_수_있다() throws Exception {
        mvc.perform(get("/api/courses").with(as("s", Roles.STUDENT)))
                .andExpect(status().isOk());
    }

    @Test
    void 강사는_수강신청할_수_없다() throws Exception {
        // @PreAuthorize가 메서드 진입 전에 차단 → 임의 courseId여도 403
        mvc.perform(post("/api/courses/" + UUID.randomUUID() + "/enrollments").with(as("i", Roles.INSTRUCTOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 학생은_수강신청할_수_있다() throws Exception {
        // 시드된 A 과정 id 하나 확보 (조회는 학생도 허용)
        String body = mvc.perform(get("/api/courses").with(as("s", Roles.STUDENT)))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> courses = om.readValue(body, new TypeReference<>() {});
        String courseId = (String) courses.get(0).get("id");

        mvc.perform(post("/api/courses/" + courseId + "/enrollments").with(as("alice", Roles.STUDENT)))
                .andExpect(status().isCreated());
    }
}
