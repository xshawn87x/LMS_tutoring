package com.lms.error;

import java.time.OffsetDateTime;

/** 표준 에러 응답 본문. 모든 에러는 이 형식으로 직렬화된다. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
