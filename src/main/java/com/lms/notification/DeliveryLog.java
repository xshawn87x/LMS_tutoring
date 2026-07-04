package com.lms.notification;

import com.lms.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 채널별 발송 이력. SMS/카카오는 스텁이라 status=SIMULATED. */
@Entity
@Table(name = "delivery_log")
@Getter
@NoArgsConstructor
public class DeliveryLog extends TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column
    private String title;

    @Column(nullable = false)
    private String status;   // SENT | SIMULATED | FAILED

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public DeliveryLog(NotificationChannel channel, String recipient, String title, String status) {
        this.channel = channel;
        this.recipient = recipient;
        this.title = title;
        this.status = status;
        this.createdAt = OffsetDateTime.now();
    }
}
