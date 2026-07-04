package com.lms.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * 현재 요청을 처리하는 스레드의 테넌트를 보관한다.
 * TenantFilter가 JWT에서 추출해 채우고, TenantAwareDataSource가 읽어
 * DB 세션 변수(app.current_tenant)로 밀어넣는다.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
