package com.lms.notification;

import com.lms.error.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 알림 발송/조회. 채널별 {@link MessageSender}로 발송하고 delivery_log에 이력을 남긴다.
 * 다른 모듈은 {@link #notify}로 인앱 알림을 보낼 수 있다(예: 과제 채점 시).
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final Map<NotificationChannel, MessageSender> senders;

    public NotificationService(NotificationRepository notificationRepository,
                               DeliveryLogRepository deliveryLogRepository,
                               List<MessageSender> senderList) {
        this.notificationRepository = notificationRepository;
        this.deliveryLogRepository = deliveryLogRepository;
        this.senders = senderList.stream().collect(Collectors.toMap(MessageSender::channel, Function.identity()));
    }

    /** 인앱 알림(가장 흔한 경로). 다른 서비스가 이벤트 시 호출. */
    public void notify(String recipient, String title, String body) {
        dispatch(recipient, title, body, NotificationChannel.IN_APP);
    }

    /** 지정 채널로 발송하고 이력을 남긴다. */
    public String dispatch(String recipient, String title, String body, NotificationChannel channel) {
        MessageSender sender = senders.get(channel);
        String status = (sender == null) ? "FAILED" : sender.send(recipient, title, body);
        deliveryLogRepository.save(new DeliveryLog(channel, recipient, title, status));
        return status;
    }

    @Transactional(readOnly = true)
    public List<Notification> myNotifications(String recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String recipient) {
        return notificationRepository.countByRecipientAndReadFalse(recipient);
    }

    public void markRead(UUID id, String recipient) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다"));
        // RLS로 이미 테넌트 격리 + 본인 것만 읽음 처리
        if (n.getRecipient().equals(recipient)) {
            n.markRead();
        }
    }

    public void markAllRead(String recipient) {
        notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient).forEach(Notification::markRead);
    }

    @Transactional(readOnly = true)
    public List<DeliveryLog> recentLogs(int limit) {
        return deliveryLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }
}
