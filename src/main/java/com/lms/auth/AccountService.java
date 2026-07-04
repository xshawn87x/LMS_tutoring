package com.lms.auth;

import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import com.lms.error.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인한 사용자의 계정 관리(비밀번호 변경·표시 이름 수정).
 * 실제 가입 계정(app_user)에만 적용된다 — dev 토큰 사용자는 계정이 없어 404.
 * 현재 테넌트는 요청의 JWT로 이미 설정되어 있어 RLS가 본인 테넌트로 격리한다.
 */
@Service
@Transactional
public class AccountService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("새 비밀번호는 8자 이상이어야 합니다");
        }
        AppUser user = requireUser(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    public AppUser updateProfile(String email, String displayName) {
        AppUser user = requireUser(email);
        user.rename(displayName == null || displayName.isBlank() ? null : displayName.trim());
        return user;
    }

    private AppUser requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("계정을 찾을 수 없습니다 (dev 토큰 사용자는 계정 관리가 불가합니다)"));
    }
}
