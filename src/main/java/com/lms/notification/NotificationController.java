package com.lms.notification;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 알림 API. 내 알림=본인, 발송/이력=INSTRUCTOR/ADMIN. */
@RestController
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    // --- 내 알림 ---

    @GetMapping("/api/me/notifications")
    public List<NotificationView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.myNotifications(jwt.getSubject()).stream().map(NotificationView::from).toList();
    }

    @GetMapping("/api/me/notifications/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", service.unreadCount(jwt.getSubject()));
    }

    @PostMapping("/api/me/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.markRead(id, jwt.getSubject());
    }

    @PostMapping("/api/me/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(jwt.getSubject());
    }

    // --- 발송(관리자) ---

    @PostMapping("/api/admin/notifications/send")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Map<String, String> send(@RequestBody SendRequest req) {
        NotificationChannel channel = req.channel() == null ? NotificationChannel.IN_APP
                : NotificationChannel.valueOf(req.channel().trim().toUpperCase());
        String status = service.dispatch(req.recipient(), req.title(), req.body(), channel);
        return Map.of("status", status);
    }

    @GetMapping("/api/admin/notifications/logs")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<DeliveryLogView> logs() {
        return service.recentLogs(100).stream().map(DeliveryLogView::from).toList();
    }

    // --- DTO ---

    public record SendRequest(@NotBlank String recipient, @NotBlank String title, String body, String channel) {
    }

    public record NotificationView(UUID id, String title, String body, String category, boolean read, OffsetDateTime createdAt) {
        static NotificationView from(Notification n) {
            return new NotificationView(n.getId(), n.getTitle(), n.getBody(), n.getCategory(), n.isRead(), n.getCreatedAt());
        }
    }

    public record DeliveryLogView(UUID id, String channel, String recipient, String title, String status, OffsetDateTime createdAt) {
        static DeliveryLogView from(DeliveryLog d) {
            return new DeliveryLogView(d.getId(), d.getChannel().name(), d.getRecipient(), d.getTitle(), d.getStatus(), d.getCreatedAt());
        }
    }
}
