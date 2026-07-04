package com.lms.platform;

import com.lms.error.ApiException;
import org.springframework.http.HttpStatus;

/** 정지/연체된 기관이 기능을 이용하려 할 때 (403). */
public class AccountSuspendedException extends ApiException {
    public AccountSuspendedException(TenantStatus status) {
        super(HttpStatus.FORBIDDEN, status == TenantStatus.PAST_DUE
                ? "연체된 기관입니다. 미납 요금을 결제해야 이용할 수 있습니다"
                : "정지된 기관입니다. 관리자에게 문의하세요");
    }
}
