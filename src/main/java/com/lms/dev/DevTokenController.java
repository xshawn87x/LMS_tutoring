package com.lms.dev;

import com.lms.security.Roles;
import com.lms.security.TokenService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * (dev 프로파일 전용) 테스트용 JWT 발급기.
 *
 * 임의의 tenant_id로 토큰을 만들어 서로 다른 테넌트의 격리를 손쉽게 검증한다.
 * 운영 환경에는 절대 노출되지 않도록 dev 프로파일에서만 빈으로 등록된다.
 */
@RestController
@Profile("dev")
public class DevTokenController {

    private final TokenService tokenService;

    public DevTokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/dev/token")
    public Map<String, String> token(@RequestParam("tenantId") String tenantId,
                                     @RequestParam(value = "subject", defaultValue = "dev-user") String subject,
                                     @RequestParam(value = "roles", defaultValue = Roles.STUDENT) List<String> roles) {
        return Map.of("token", tokenService.issue(subject, tenantId, roles));
    }
}
