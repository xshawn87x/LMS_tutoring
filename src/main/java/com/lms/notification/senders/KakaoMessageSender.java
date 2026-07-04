package com.lms.notification.senders;

import com.lms.notification.MessageSender;
import com.lms.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 카카오 알림톡 발송 스텁. 외부 유료 연동 전이라 SIMULATED. */
@Component
public class KakaoMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(KakaoMessageSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public String send(String recipient, String title, String body) {
        log.info("[KAKAO-STUB] to={} title={} (실발송 미연동)", recipient, title);
        return "SIMULATED";
    }
}
