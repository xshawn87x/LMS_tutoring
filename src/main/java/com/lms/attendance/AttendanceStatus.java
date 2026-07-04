package com.lms.attendance;

/** 출석 상태. */
public enum AttendanceStatus {
    PRESENT,   // 출석
    ABSENT,    // 결석
    LATE,      // 지각
    EXCUSED    // 사유결석(공결)
}
