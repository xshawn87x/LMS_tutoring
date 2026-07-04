package com.lms.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 인증된 JWT의 tenant_id 클레임을 읽어 TenantContext에 채운다.
 *
 * <b>반드시 Spring Security의 인증 필터(BearerTokenAuthenticationFilter) 이후에 실행돼야 한다</b> —
 * 그래야 SecurityContext에 Authentication(JWT)이 들어있다. 그래서 @Component로 자동 등록하지 않고
 * SecurityConfig가 보안 필터 체인 안 인증 필터 다음 위치에 직접 끼워넣는다.
 */
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            resolveTenant().ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            // 스레드 풀 재사용 시 누수 방지
            TenantContext.clear();
        }
    }

    private java.util.Optional<UUID> resolveTenant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String tenant = jwt.getClaimAsString(TENANT_CLAIM);
            if (tenant != null && !tenant.isBlank()) {
                return java.util.Optional.of(UUID.fromString(tenant));
            }
        }
        return java.util.Optional.empty();
    }
}
