package com.lms.auth;

import com.lms.auth.dto.AuthDtos.AccountResponse;
import com.lms.auth.dto.AuthDtos.ChangePasswordRequest;
import com.lms.auth.dto.AuthDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 사용자의 계정 관리 API. 학생 식별자(email)는 JWT subject에서 가져온다.
 * 실제 가입 계정에만 적용(dev 토큰 사용자는 404).
 */
@RestController
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping("/api/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req, @AuthenticationPrincipal Jwt jwt) {
        service.changePassword(jwt.getSubject(), req.currentPassword(), req.newPassword());
    }

    @PutMapping("/api/me/account")
    public AccountResponse updateProfile(@Valid @RequestBody UpdateProfileRequest req,
                                         @AuthenticationPrincipal Jwt jwt) {
        AppUser user = service.updateProfile(jwt.getSubject(), req.displayName());
        return new AccountResponse(user.getEmail(), user.getDisplayName(), user.roleList());
    }
}
