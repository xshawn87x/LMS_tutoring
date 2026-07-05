package com.lms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 운영(prod) 프로파일에서만 활성화되는 기동 가드.
 *
 * <p>기본/약한 시크릿(로컬 개발용 값)이 그대로 운영에 배포되는 사고를 막기 위해,
 * 부팅 시점에 JWT 시크릿·플랫폼 관리자 비밀번호·DB 비밀번호가 안전한지 검사하고
 * 미흡하면 애플리케이션 기동을 중단한다(fail-fast).
 *
 * <p>운영 배포 시 반드시 환경변수로 강한 값을 주입할 것:
 * LMS_JWT_SECRET, LMS_PLATFORM_ADMIN_PASSWORD.
 * (DB 롤 비밀번호는 마이그레이션이 설정하므로 여기서 검사하지 않는다 — 네트워크 격리 + 수동 교체로 관리, DEPLOY.md 참고.)
 */
@Component
@Profile("prod")
public class ProductionReadinessGuard {

    /** application.yml의 로컬 개발용 기본 시크릿(운영에 나가면 안 되는 값). */
    static final String DEV_JWT_SECRET = "dev-only-secret-change-me-please-32bytes-minimum!!";

    private final String jwtSecret;
    private final String platformPassword;

    public ProductionReadinessGuard(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${platform.admin.password:}") String platformPassword) {
        this.jwtSecret = jwtSecret;
        this.platformPassword = platformPassword;
    }

    @PostConstruct
    void check() {
        validate(jwtSecret, platformPassword);
    }

    /** 안전하지 않으면 IllegalStateException. (단위 테스트를 위해 static·순수 함수) */
    static void validate(String jwtSecret, String platformPassword) {
        List<String> problems = new ArrayList<>();

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.equals(DEV_JWT_SECRET) || jwtSecret.length() < 32) {
            problems.add("LMS_JWT_SECRET 미설정/약함 — 32자 이상 무작위 시크릿을 주입하세요.");
        }
        if (platformPassword == null || platformPassword.isBlank()
                || platformPassword.equals("1") || platformPassword.length() < 8) {
            problems.add("LMS_PLATFORM_ADMIN_PASSWORD 약함 — 8자 이상 강한 비밀번호를 주입하세요.");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "운영(prod) 실행이 차단되었습니다 — 보안 설정 미흡:\n - " + String.join("\n - ", problems)
                            + "\n환경변수로 강한 값을 주입한 뒤 다시 실행하세요.");
        }
    }
}
