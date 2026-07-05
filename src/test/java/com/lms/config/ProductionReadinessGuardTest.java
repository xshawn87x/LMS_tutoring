package com.lms.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 운영 기동 가드 — 약한/기본 시크릿이면 기동 차단, 강하면 통과. (순수 단위 테스트) */
class ProductionReadinessGuardTest {

    private static final String STRONG_SECRET = "a-very-long-random-production-secret-0123456789abcdef";
    private static final String STRONG_PW = "S3cure!Platform#pw";

    @Test
    void 강한_시크릿이면_통과() {
        assertThatCode(() -> ProductionReadinessGuard.validate(STRONG_SECRET, STRONG_PW))
                .doesNotThrowAnyException();
    }

    @Test
    void 기본_JWT_시크릿이면_차단() {
        assertThatThrownBy(() -> ProductionReadinessGuard.validate(ProductionReadinessGuard.DEV_JWT_SECRET, STRONG_PW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 짧은_JWT_시크릿이면_차단() {
        assertThatThrownBy(() -> ProductionReadinessGuard.validate("short", STRONG_PW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 약한_플랫폼_비밀번호면_차단() {
        assertThatThrownBy(() -> ProductionReadinessGuard.validate(STRONG_SECRET, "1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
