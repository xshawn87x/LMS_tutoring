package com.lms.auth;

import com.lms.auth.dto.AuthDtos.AuthResponse;
import com.lms.auth.dto.AuthDtos.LoginRequest;
import com.lms.auth.dto.AuthDtos.PasswordResetConfirm;
import com.lms.auth.dto.AuthDtos.RegisterRequest;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.error.UnauthorizedException;
import com.lms.security.Roles;
import com.lms.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 회원가입/로그인. 비밀번호는 bcrypt로 해시해 저장하고, 검증 후 HS256 JWT를 발급한다.
 *
 * <p>중요: 테넌트 조회({@link #resolveTenant})는 별도 트랜잭션에서 수행해, 그 다음 호출 전에
 * TenantContext를 세팅하면 app_user 쿼리가 올바른 테넌트로 RLS 격리되도록 한다.
 * (TenantAwareDataSource는 커넥션 획득 시점의 TenantContext로 app.current_tenant를 설정한다.)
 */
@Service
public class AuthService {

    /** 자가 가입으로 허용하는 역할 (ADMIN은 자가 가입 불가). */
    private static final Set<String> SELF_SIGNUP_ROLES = Set.of(Roles.STUDENT, Roles.INSTRUCTOR, Roles.PARENT);

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(AppUserRepository userRepository, TenantRepository tenantRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** org_code로 테넌트를 찾는다 (RLS 없는 전역 레지스트리). 다음 단계 전에 TenantContext를 세팅하기 위함. */
    @Transactional(readOnly = true)
    public Tenant resolveTenant(String orgCode) {
        return tenantRepository.findByOrgCode(orgCode.trim())
                .orElseThrow(() -> new NotFoundException("등록되지 않은 기관 코드입니다: " + orgCode));
    }

    /** 회원가입. 호출 전에 TenantContext가 해당 테넌트로 세팅되어 있어야 한다. */
    @Transactional
    public AuthResponse register(Tenant tenant, RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("이미 가입된 이메일입니다");
        }
        String role = (req.role() == null || req.role().isBlank()) ? Roles.STUDENT : req.role().trim().toUpperCase();
        if (!SELF_SIGNUP_ROLES.contains(role)) {
            throw new ConflictException("가입할 수 없는 역할입니다: " + role);
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(req.password()),
                blankToNull(req.displayName()), role);
        userRepository.save(user);
        return toResponse(tenant, user);
    }

    /** 로그인. 호출 전에 TenantContext가 해당 테넌트로 세팅되어 있어야 한다. */
    @Transactional(readOnly = true)
    public AuthResponse login(Tenant tenant, LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        AppUser user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다"));
        return toResponse(tenant, user);
    }

    /**
     * 비밀번호 재설정 토큰 발급. 호출 전 TenantContext가 세팅되어 있어야 한다.
     * 계정이 없으면 404(로컬 편의). 발급된 토큰 문자열을 반환한다(운영에선 이메일로 전달, 응답에 노출 안 함).
     */
    @Transactional
    public String requestPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(email)) {
            throw new NotFoundException("해당 이메일의 계정이 없습니다");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        resetTokenRepository.save(new PasswordResetToken(email, token, OffsetDateTime.now().plusHours(1)));
        return token;
    }

    /** 토큰으로 비밀번호 확정. 호출 전 TenantContext가 세팅되어 있어야 한다. */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirm req) {
        String email = req.email().trim().toLowerCase();
        PasswordResetToken token = resetTokenRepository.findByEmailAndToken(email, req.token().trim())
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BadRequestException("유효하지 않거나 만료된 토큰입니다"));
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다"));
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        token.markUsed();
    }

    private AuthResponse toResponse(Tenant tenant, AppUser user) {
        List<String> roles = user.roleList();
        String token = tokenService.issue(user.getEmail(), tenant.getId().toString(), roles);
        return new AuthResponse(token, user.getEmail(), tenant.getId().toString(),
                tenant.getOrgCode(), user.getDisplayName(), roles);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
