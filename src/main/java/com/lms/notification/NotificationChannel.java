package com.lms.notification;

/** 알림 채널. IN_APP·EMAIL은 실제 동작(EMAIL은 SMTP 설정 시), SMS/KAKAO는 외부 연동 스텁(SIMULATED). */
public enum NotificationChannel {
    IN_APP,
    EMAIL,
    SMS,
    KAKAO
}
