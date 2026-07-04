package com.lms.feature;

import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 테넌트별 기능 플래그 API.
 * 조회는 인증된 누구나(프론트가 UI를 게이팅하기 위해). 변경은 ADMIN(기관 관리자)만.
 */
@RestController
@RequestMapping("/api/features")
public class FeatureController {

    private final FeatureService service;

    public FeatureController(FeatureService service) {
        this.service = service;
    }

    @GetMapping
    public List<FeatureView> list() {
        return service.list();
    }

    @PutMapping("/{feature}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeatureView> toggle(@PathVariable Feature feature, @RequestBody ToggleRequest request) {
        service.setEnabled(feature, request.enabled());
        return service.list();
    }

    public record ToggleRequest(@NotNull Boolean enabled) {
    }
}
