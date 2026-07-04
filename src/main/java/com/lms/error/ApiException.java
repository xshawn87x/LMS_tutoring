package com.lms.error;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 도메인 예외의 공통 상위 타입. HTTP 상태를 함께 들고 다녀,
 * GlobalExceptionHandler가 일관된 형식으로 응답을 만들 수 있게 한다.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
