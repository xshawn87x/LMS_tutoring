package com.lms.platform.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 감사 로그 기록/조회. 행위자(actor)는 현재 인증 컨텍스트(플랫폼 슈퍼관리자 JWT subject)에서 읽는다.
 * 인증 컨텍스트가 없으면(스케줄러/시스템 작업) 'system'으로 기록한다.
 */
@Service
@Transactional
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String targetType, String targetId, String detail) {
        repository.save(new AuditLog(currentActor(), action, targetType, targetId, detail));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recent(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> forTarget(String targetId, int limit) {
        return repository.findByTargetIdOrderByCreatedAtDesc(targetId, PageRequest.of(0, limit));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }
        return "system";
    }
}
