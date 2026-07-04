package com.lms.admin.dto;

import com.lms.auth.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MemberDtos {

    private MemberDtos() {
    }

    public record CreateMemberRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            String displayName,
            @NotEmpty List<String> roles) {
    }

    public record UpdateRolesRequest(@NotEmpty List<String> roles) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 8) String newPassword) {
    }

    public record MemberResponse(
            UUID id, String email, String displayName, List<String> roles, OffsetDateTime createdAt) {
        public static MemberResponse from(AppUser u) {
            return new MemberResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.roleList(), u.getCreatedAt());
        }
    }
}
