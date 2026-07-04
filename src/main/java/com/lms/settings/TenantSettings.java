package com.lms.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 학원(테넌트) 환경설정. 테넌트당 1행(PK=tenant_id). RLS로 격리. */
@Entity
@Table(name = "tenant_settings")
@Getter
@NoArgsConstructor
public class TenantSettings {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column
    private String contact;

    @Column
    private String terms;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public TenantSettings(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void apply(String displayName, String logoUrl, String primaryColor, String contact, String terms) {
        this.displayName = displayName;
        this.logoUrl = logoUrl;
        this.primaryColor = primaryColor;
        this.contact = contact;
        this.terms = terms;
        this.updatedAt = OffsetDateTime.now();
    }
}
