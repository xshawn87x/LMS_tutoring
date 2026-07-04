package com.lms.error;

import org.springframework.http.HttpStatus;

/** 권한/소유 위반 (403). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
