package com.lms.error;

import org.springframework.http.HttpStatus;

/** 리소스를 찾을 수 없음 (404). RLS로 타 테넌트 자원이 숨겨진 경우도 포함. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
