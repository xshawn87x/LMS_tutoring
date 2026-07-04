package com.lms.dashboard.dto;

import java.util.UUID;

/** 강사 대시보드의 과정별 집계 한 줄. */
public record CourseStatsResponse(
        UUID courseId,
        String title,
        String categoryCode,
        Integer level,
        long enrollmentCount,
        int avgProgress,
        long completedCount,
        int quizCount,
        Integer avgQuizScore,   // 모든 제출 평균 정답률(%) — 제출 없으면 null
        long certificateCount
) {
}
