package com.lms.auth;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 계정. 비밀번호는 bcrypt 해시로만 저장한다.
 * tenant_id는 {@link TenantOwned}가 채우고 RLS가 격리한다.
 */
@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor
public class AppUser extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    /** 콤마 구분 역할 문자열 (예: "STUDENT" 또는 "INSTRUCTOR,ADMIN"). */
    @Column(nullable = false)
    private String roles;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AppUser(String email, String passwordHash, String displayName, String roles) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.roles = roles;
    }

    public List<String> roleList() {
        return Arrays.stream(roles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 비밀번호 변경 (이미 bcrypt로 해시된 값). */
    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    /** 표시 이름 변경. */
    public void rename(String displayName) {
        this.displayName = displayName;
    }

    /** 역할 변경 (콤마 구분 CSV). */
    public void changeRoles(String rolesCsv) {
        this.roles = rolesCsv;
    }
}
