package com.lms.platform;

import com.lms.error.ApiException;
import com.lms.feature.Feature;
import org.springframework.http.HttpStatus;

/** 요금제(자격)에 포함되지 않은 기능을 켜려 했을 때 (403). 자격은 플랫폼만 부여할 수 있다. */
public class FeatureNotEntitledException extends ApiException {
    public FeatureNotEntitledException(Feature feature) {
        super(HttpStatus.FORBIDDEN, "요금제에 포함되지 않은 기능입니다: " + feature.name());
    }
}
