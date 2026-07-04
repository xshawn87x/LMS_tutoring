package com.lms.error;

import org.springframework.http.HttpStatus;

/** 상태 충돌 (409). 예: 중복 수강신청. */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
