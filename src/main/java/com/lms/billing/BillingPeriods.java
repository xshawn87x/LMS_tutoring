package com.lms.billing;

import java.time.YearMonth;

/** 청구 주기(월) 유틸. 형식은 'YYYY-MM'. */
public final class BillingPeriods {

    private BillingPeriods() {
    }

    /** 이번 달 청구 주기(예: "2026-07"). */
    public static String current() {
        return YearMonth.now().toString();
    }
}
