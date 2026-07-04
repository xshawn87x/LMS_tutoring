package com.lms.curation;

import java.util.List;

/**
 * 과정 콘텐츠를 분석해 태그·난이도·요약·예상시간을 산출한다.
 * 구현: {@link HeuristicContentAnalyzer}(기본, 무료) / ClaudeContentAnalyzer(API 키 설정 시).
 */
public interface ContentAnalyzer {

    ContentAnalysis analyze(AnalyzeInput input);

    /** 결과 출처 표시용 (HEURISTIC | CLAUDE). */
    String name();

    /** 이 분석기를 실제로 쓸 수 있는지 (예: Claude는 API 키가 있어야 true). */
    default boolean isAvailable() {
        return true;
    }

    record AnalyzeInput(
            String title,
            String description,
            String categoryCode,
            Integer declaredLevel,
            List<String> lessonTitles,
            List<String> lessonContents
    ) {
    }

    record ContentAnalysis(
            List<String> tags,
            Integer difficulty,
            String summary,
            Integer estMinutes
    ) {
    }
}
