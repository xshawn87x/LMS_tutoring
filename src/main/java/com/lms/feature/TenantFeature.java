package com.lms.feature;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 한 테넌트의 한 기능에 대한 on/off override. 없으면 {@link Feature#isDefaultEnabled()}를 따른다. */
@Entity
@Table(name = "tenant_feature")
@Getter
@NoArgsConstructor
public class TenantFeature extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Feature feature;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public TenantFeature(Feature feature, boolean enabled) {
        this.feature = feature;
        this.enabled = enabled;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
