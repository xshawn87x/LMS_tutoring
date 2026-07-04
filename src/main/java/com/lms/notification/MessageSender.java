package com.lms.notification;

/**
 * 알림 발송 이음새. 채널별 구현이 실제 발송을 담당한다.
 * IN_APP만 실제 동작(알림 저장). SMS/KAKAO는 외부 유료 연동이 필요해 스텁(SIMULATED)이며,
 * 운영에선 이 인터페이스를 구현하는 실제 발송기(예: NHN/Kakao 비즈메시지)로 빈을 교체한다.
 */
public interface MessageSender {

    NotificationChannel channel();

    /** 발송하고 상태(SENT | SIMULATED | FAILED)를 반환. */
    String send(String recipient, String title, String body);
}
