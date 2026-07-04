package com.lms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 활성화 (청구 자동 마감 등). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
