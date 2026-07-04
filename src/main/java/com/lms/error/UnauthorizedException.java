package com.lms.error;

import org.springframework.http.HttpStatus;

/** 인증 실패 (401). 예: 잘못된 이메일/비밀번호. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
