package com.lms.notification;

/** 알림 채널. IN_APP만 실제 동작, SMS/KAKAO는 외부 연동 스텁(SIMULATED). */
public enum NotificationChannel {
    IN_APP,
    SMS,
    KAKAO
}
