package com.lms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI(/swagger-ui.html)에서 JWT Bearer 토큰을 넣어 API를 바로 호출할 수 있게 한다.
 *
 * 사용법: /dev/token?tenantId=... 으로 토큰을 받아 우측 상단 "Authorize"에 붙여넣으면,
 * 이후 모든 요청에 Authorization: Bearer ... 가 자동으로 실린다 → 테넌트별 격리를 화면에서 확인.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    public OpenAPI lmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LMS API — Step 1 (RLS 멀티테넌시)")
                        .version("0.0.1")
                        .description("""
                                테넌트별 행 단위 격리(PostgreSQL RLS) 검증용 API.

                                1) GET /dev/token?tenantId=11111111-1111-1111-1111-111111111111 로 토큰 발급
                                2) 우측 상단 **Authorize** 에 토큰 입력
                                3) GET /api/courses 호출 → 해당 테넌트의 과정만 보임
                                """))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
