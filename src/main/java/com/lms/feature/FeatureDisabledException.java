package com.lms.feature;

import com.lms.error.ApiException;
import org.springframework.http.HttpStatus;

/** 해당 기관(테넌트)에서 꺼져 있는 기능을 호출했을 때 (403). */
public class FeatureDisabledException extends ApiException {
    public FeatureDisabledException(Feature feature) {
        super(HttpStatus.FORBIDDEN, "이 기관에서 비활성화된 기능입니다: " + feature.name());
    }
}
