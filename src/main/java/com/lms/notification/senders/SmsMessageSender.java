package com.lms.notification.senders;

import com.lms.notification.MessageSender;
import com.lms.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** SMS 발송 스텁. 외부 유료(예: NHN Cloud SMS) 연동 전이라 실제 발송 없이 SIMULATED. */
@Component
public class SmsMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(SmsMessageSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public String send(String recipient, String title, String body) {
        log.info("[SMS-STUB] to={} title={} (실발송 미연동)", recipient, title);
        return "SIMULATED";
    }
}
