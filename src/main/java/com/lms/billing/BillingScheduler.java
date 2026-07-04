package com.lms.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * 청구 자동화. 매월 1일 새벽 지난달을 마감(모든 기관 인보이스 발행 + 연체 판정)한다.
 * 사용량 카운터는 period('YYYY-MM') 키라 달이 바뀌면 자동으로 새 주기가 시작된다(별도 리셋 불필요).
 */
@Component
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private final BillingService billingService;

    public BillingScheduler(BillingService billingService) {
        this.billingService = billingService;
    }

    /** 매월 1일 02:00 — 지난달 청구 마감. */
    @Scheduled(cron = "0 0 2 1 * *")
    public void closeLastMonth() {
        String lastMonth = YearMonth.now().minusMonths(1).toString();
        var issued = billingService.closePeriod(lastMonth);
        log.info("청구 마감 완료: {} · 인보이스 {}건 발행", lastMonth, issued.size());
    }
}
