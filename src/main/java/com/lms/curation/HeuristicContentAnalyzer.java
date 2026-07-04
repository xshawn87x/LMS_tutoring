package com.lms.curation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 무료 휴리스틱 분석기 (기본). 키워드 사전으로 태그를 뽑고, 선언 난이도/키워드로 난이도를 추정하며,
 * 설명/첫 레슨으로 요약을, 레슨 수로 예상시간을 만든다. AI 키가 없어도 동작한다.
 * (Claude 분석기가 빈으로 존재하면 그쪽이 우선 사용됨)
 */
@Component
public class HeuristicContentAnalyzer implements ContentAnalyzer {

    // 키워드 → 태그 (소문자 비교)
    private static final Map<String, String> KEYWORD_TAGS = Map.ofEntries(
            Map.entry("spring", "spring"),
            Map.entry("스프링", "spring"),
            Map.entry("jpa", "jpa"),
            Map.entry("hibernate", "jpa"),
            Map.entry("java", "java"),
            Map.entry("자바", "java"),
            Map.entry("python", "python"),
            Map.entry("파이썬", "python"),
            Map.entry("react", "react"),
            Map.entry("리액트", "react"),
            Map.entry("sql", "sql"),
            Map.entry("database", "database"),
            Map.entry("데이터베이스", "database"),
            Map.entry("docker", "docker"),
            Map.entry("kubernetes", "kubernetes"),
            Map.entry("aws", "cloud"),
            Map.entry("api", "api"),
            Map.entry("rest", "api")
    );

    private static final int MAX_TAGS = 6;

    @Override
    public String name() {
        return "HEURISTIC";
    }

    @Override
    public ContentAnalysis analyze(AnalyzeInput in) {
        String haystack = String.join(" \n ",
                safe(in.title()), safe(in.description()),
                String.join(" ", nullSafe(in.lessonTitles())),
                String.join(" ", nullSafe(in.lessonContents()))
        ).toLowerCase();

        Set<String> tags = new LinkedHashSet<>();
        if (in.categoryCode() != null && !in.categoryCode().isBlank()) {
            tags.add(in.categoryCode().toLowerCase());
        }
        for (var entry : KEYWORD_TAGS.entrySet()) {
            if (haystack.contains(entry.getKey())) {
                tags.add(entry.getValue());
            }
            if (tags.size() >= MAX_TAGS) break;
        }

        Integer difficulty = estimateDifficulty(in, haystack);
        String summary = buildSummary(in);
        int lessonCount = nullSafe(in.lessonTitles()).size();
        Integer estMinutes = Math.max(10, lessonCount * 10);

        return new ContentAnalysis(new ArrayList<>(tags), difficulty, summary, estMinutes);
    }

    private Integer estimateDifficulty(AnalyzeInput in, String haystack) {
        if (in.declaredLevel() != null) {
            return in.declaredLevel();
        }
        if (haystack.contains("고급") || haystack.contains("심화") || haystack.contains("advanced")) return 3;
        if (haystack.contains("중급") || haystack.contains("intermediate")) return 2;
        if (haystack.contains("입문") || haystack.contains("기초") || haystack.contains("beginner")) return 0;
        return 1;
    }

    private String buildSummary(AnalyzeInput in) {
        String base = (in.description() != null && !in.description().isBlank())
                ? in.description()
                : nullSafe(in.lessonContents()).stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse("");
        if (base.isBlank()) {
            return safe(in.title()) + " 과정";
        }
        base = base.strip().replaceAll("\\s+", " ");
        return base.length() <= 140 ? base : base.substring(0, 140) + "…";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}
