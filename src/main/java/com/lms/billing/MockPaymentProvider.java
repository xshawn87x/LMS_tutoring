package com.lms.billing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬/데모용 결제 프로바이더. 외부 호출 없이 즉시 성공 처리하고 모의 결제 참조를 반환한다.
 *
 * <p>기본값(app.payment.provider 미설정 또는 "mock")에서 활성화된다. 운영에서 실 PG를 붙이려면
 * {@code app.payment.provider=toss}(또는 iamport)로 두고 해당 {@link PaymentProvider} 구현 빈
 * (예: TossPaymentProvider)을 추가하면 이 빈은 비활성화되고 청구 로직(BillingService)은 그대로 재사용된다.
 * 실 연동 절차는 DEPLOY.md 참고(토스페이먼츠 결제 승인 API).
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
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
