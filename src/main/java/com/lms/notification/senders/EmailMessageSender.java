package com.lms.notification.senders;

import com.lms.notification.MessageSender;
import com.lms.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 이메일 발송기 — 실제 동작하는 발송기.
 * SMTP가 설정되면(spring.mail.host 등 환경변수) JavaMailSender로 실제 전송하고,
 * 설정이 없으면(로컬/미구성) SIMULATED를 반환한다. 수신자(recipient)는 학부모 이메일(=subject).
 *
 * 운영 활성화: SPRING_MAIL_HOST / SPRING_MAIL_PORT / SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD 주입.
 */
@Component
public class EmailMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(EmailMessageSender.class);

    private final ObjectProvider<JavaMailSender> mailProvider;
    private final String from;

    public EmailMessageSender(ObjectProvider<JavaMailSender> mailProvider,
                              @Value("${app.mail.from:no-reply@lms.local}") String from) {
        this.mailProvider = mailProvider;
        this.from = from;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String send(String recipient, String title, String body) {
        JavaMailSender mail = mailProvider.getIfAvailable();
        if (mail == null) {
            // SMTP 미설정 — 실제 발송 없이 SIMULATED (SMS/Kakao 스텁과 동일한 취급)
            log.info("[EMAIL-SIM] to={} subject={} (SMTP 미설정)", recipient, title);
            return "SIMULATED";
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(recipient);
            msg.setSubject(title);
            msg.setText(body == null ? "" : body);
            mail.send(msg);
            return "SENT";
        } catch (Exception e) {
            log.warn("[EMAIL] 발송 실패 to={} : {}", recipient, e.getMessage());
            return "FAILED";
        }
    }
}
