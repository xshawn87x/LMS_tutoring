package com.lms.notification.senders;

import com.lms.notification.MessageSender;
import com.lms.notification.Notification;
import com.lms.notification.NotificationChannel;
import com.lms.notification.NotificationRepository;
import org.springframework.stereotype.Component;

/** 인앱 알림 발송 = notification 저장. 유일하게 실제 동작하는 채널. */
@Component
public class InAppMessageSender implements MessageSender {

    private final NotificationRepository repository;

    public InAppMessageSender(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public String send(String recipient, String title, String body) {
        repository.save(new Notification(recipient, title, body, "GENERAL"));
        return "SENT";
    }
}
