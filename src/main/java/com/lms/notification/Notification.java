package com.lms.notification;

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
import java.util.UUID;

/** 인앱 알림. */
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
public class Notification extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String title;

    @Column
    private String body;

    @Column
    private String category;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Notification(String recipient, String title, String body, String category) {
        this.recipient = recipient;
        this.title = title;
        this.body = body;
        this.category = category;
        this.read = false;
        this.createdAt = OffsetDateTime.now();
    }

    public void markRead() {
        this.read = true;
    }
}
