package com.lms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 인증 관련 요청/응답 DTO 모음. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String orgCode,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
            String displayName,
            String role   // null이면 STUDENT
    ) {
    }

    public record LoginRequest(
            @NotBlank String orgCode,
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String token,
            String subject,     // = email (JWT subject)
            String tenantId,
            String orgCode,
            String displayName,
            List<String> roles
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다") String newPassword
    ) {
    }

    public record UpdateProfileRequest(
            String displayName
    ) {
    }

    /** 내 계정 정보 (조회/수정 응답). */
    public record AccountResponse(
            String email,
            String displayName,
            List<String> roles
    ) {
    }

    /** 비밀번호 재설정 요청(토큰 발급). */
    public record PasswordResetRequest(
            @NotBlank String orgCode,
            @NotBlank @Email String email
    ) {
    }

    /** 로컬 환경: 토큰을 응답으로 돌려준다(운영에선 이메일/문자로 전달). */
    public record PasswordResetTokenResponse(
            String token,
            String message
    ) {
    }

    /** 토큰으로 새 비밀번호 확정. */
    public record PasswordResetConfirm(
            @NotBlank String orgCode,
            @NotBlank @Email String email,
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
    ) {
    }
}
