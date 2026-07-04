package com.lms.settings;

import com.lms.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 학원 환경설정 API. 조회는 테넌트 내 누구나, 수정은 ADMIN. RLS 격리. */
@RestController
@RequestMapping("/api/settings")
@Transactional
public class SettingsController {

    private final TenantSettingsRepository repository;

    public SettingsController(TenantSettingsRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public SettingsView get() {
        UUID tenant = TenantContext.get().orElse(null);
        return repository.findAll().stream().findFirst()
                .map(SettingsView::from)
                .orElse(new SettingsView(tenant, null, null, null, null, null));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SettingsView update(@RequestBody SettingsRequest req) {
        UUID tenant = TenantContext.get().orElseThrow(() -> new IllegalStateException("테넌트 컨텍스트 없음"));
        TenantSettings s = repository.findById(tenant).orElseGet(() -> new TenantSettings(tenant));
        s.apply(req.displayName(), req.logoUrl(), req.primaryColor(), req.contact(), req.terms());
        return SettingsView.from(repository.save(s));
    }

    public record SettingsRequest(String displayName, String logoUrl, String primaryColor, String contact, String terms) {
    }

    public record SettingsView(UUID tenantId, String displayName, String logoUrl,
                               String primaryColor, String contact, String terms) {
        static SettingsView from(TenantSettings s) {
            return new SettingsView(s.getTenantId(), s.getDisplayName(), s.getLogoUrl(),
                    s.getPrimaryColor(), s.getContact(), s.getTerms());
        }
    }
}
