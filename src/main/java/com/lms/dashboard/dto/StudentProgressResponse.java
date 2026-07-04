package com.lms.dashboard.dto;

/**
 * 강사 대시보드 드릴다운: 한 과정의 수강생별 진도/성취.
 * @param quizzesTaken  응시한(제출 있는) 퀴즈 수
 * @param quizzesTotal  과정의 전체 퀴즈 수
 * @param avgQuizScore  응시한 퀴즈들의 최고점 평균(%) — 응시 없으면 null
 * @param certified     수료증 발급 여부
 */
public record StudentProgressResponse(
        String studentId,
        int progress,
        String status,
        boolean completed,
        int quizzesTaken,
        int quizzesTotal,
        Integer avgQuizScore,
        boolean certified
) {
}
