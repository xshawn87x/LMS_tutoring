package com.lms.billing;

/**
 * 결제 처리 이음새. 인보이스를 결제하고 결제 참조(ref)를 돌려준다.
 *
 * <p>현재 구현은 {@link MockPaymentProvider}(로컬·무외부호출). 실제 Stripe 연동은 이 인터페이스를
 * 구현하는 {@code StripePaymentProvider}(구독/PaymentIntent 생성)를 추가하고 빈을 교체하면 된다
 * — 나머지 청구 로직(BillingService)은 그대로 재사용된다. Stripe는 계정·시크릿 키가 필요하다.
 */
public interface PaymentProvider {

    /** 인보이스를 결제하고 결제 참조 문자열을 반환. 실패 시 예외. */
    String charge(Invoice invoice);

    /** 프로바이더 식별자(예: MOCK, STRIPE). */
    String name();
}
