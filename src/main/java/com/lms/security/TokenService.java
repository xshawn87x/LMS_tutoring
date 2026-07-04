package com.lms.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * HS256 JWT 발급 단일 출처. 로그인/회원가입(AuthService)과 dev 토큰 발급기가 공유한다.
 * 토큰에는 subject(사용자 식별), tenant_id(테넌트), roles(권한)가 담긴다.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String issue(String subject, String tenantId, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("lms")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(8)))
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim(Roles.CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * 플랫폼 슈퍼관리자 토큰. 특정 테넌트에 속하지 않으므로 tenant_id 클레임이 없다
     * (→ TenantFilter가 테넌트를 세팅하지 않음 → RLS 대상 테이블에는 접근 못 함, 전역 테이블만).
     */
    public String issuePlatform(String subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("lms")
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(8)))
                .subject(subject)
                .claim(Roles.CLAIM, List.of(Roles.PLATFORM_ADMIN))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
