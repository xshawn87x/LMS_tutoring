package com.lms.platform.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 감사 로그 응답 DTO. */
public record AuditView(
        UUID id,
        String actor,
        String action,
        String targetType,
        String targetId,
        String detail,
        OffsetDateTime createdAt) {

    public static AuditView from(AuditLog a) {
        return new AuditView(a.getId(), a.getActor(), a.getAction(),
                a.getTargetType(), a.getTargetId(), a.getDetail(), a.getCreatedAt());
    }
}
