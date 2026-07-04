package com.lms.error;

import org.springframework.http.HttpStatus;

/** 잘못된 요청 (400). 예: 정답 인덱스가 보기 범위를 벗어남, 문항 없는 퀴즈 제출. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
