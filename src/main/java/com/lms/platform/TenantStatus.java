package com.lms.platform;

/** 기관 이용 상태. ACTIVE만 기능 모듈을 이용할 수 있다. */
public enum TenantStatus {
    /** 정상. */
    ACTIVE,
    /** 연체 — 지난달 인보이스 미결제(자동 판정). 결제 시 자동 해제. */
    PAST_DUE,
    /** 수동 정지 — 슈퍼관리자가 정지. 결제로 자동 해제되지 않음. */
    SUSPENDED
}
