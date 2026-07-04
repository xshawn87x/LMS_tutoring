package com.lms.billing;

import org.springframework.stereotype.Component;

/**
 * 로컬/데모용 결제 프로바이더. 외부 호출 없이 즉시 성공 처리하고 모의 결제 참조를 반환한다.
 * 운영에서는 StripePaymentProvider로 교체(이 빈을 대체)한다.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String charge(Invoice invoice) {
        // 실제 결제 대신 결정적 모의 참조를 만든다(외부 네트워크 없음).
        return "mock_" + invoice.getPeriod().replace("-", "") + "_" + invoice.getId();
    }

    @Override
    public String name() {
        return "MOCK";
    }
}
