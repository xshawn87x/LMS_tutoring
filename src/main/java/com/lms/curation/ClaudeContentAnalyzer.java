package com.lms.curation;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Claude 기반 콘텐츠 분석기 (선택). ANTHROPIC_API_KEY가 설정된 경우에만 사용된다.
 * 키가 없으면 {@link HeuristicContentAnalyzer}가 기본으로 동작한다.
 *
 * <p>주의: 이 경로는 외부 API 키가 필요해 로컬 테스트로 검증되지 않았다(키 미설정).
 * 키를 넣으면 {@link ContentInsightService}가 이 분석기를 우선 선택한다.
 */
@Component
public class ClaudeContentAnalyzer implements ContentAnalyzer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private volatile AnthropicClient client;

    public ClaudeContentAnalyzer(@Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "CLAUDE";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ContentAnalysis analyze(AnalyzeInput in) {
        Message response = client().messages().create(MessageCreateParams.builder()
                .model("claude-opus-4-8")
                .maxTokens(1024L)
                .addUserMessage(buildPrompt(in))
                .build());

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        return parse(text);
    }

    private AnthropicClient client() {
        AnthropicClient c = this.client;
        if (c == null) {
            synchronized (this) {
                c = this.client;
                if (c == null) {
                    c = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                    this.client = c;
                }
            }
        }
        return c;
    }

    private String buildPrompt(AnalyzeInput in) {
        String lessons = String.join("\n- ", in.lessonTitles() == null ? List.of() : in.lessonTitles());
        return """
                다음 온라인 강의 과정을 분석해 JSON만 출력하세요. 설명/마크다운 없이 JSON 객체 하나만.
                형식: {"tags": string[6 이내], "difficulty": 0|1|2|3, "summary": "한 문장", "estMinutes": 정수}
                difficulty: 0 입문, 1 초급, 2 중급, 3 고급.

                제목: %s
                설명: %s
                분야코드: %s
                레슨:
                - %s
                """.formatted(
                safe(in.title()), safe(in.description()), safe(in.categoryCode()), lessons);
    }

    private ContentAnalysis parse(String text) {
        try {
            String json = text.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = MAPPER.readTree(json);
            List<String> tags = new ArrayList<>();
            if (node.has("tags")) {
                node.get("tags").forEach(t -> tags.add(t.asText()));
            }
            Integer difficulty = node.has("difficulty") ? node.get("difficulty").asInt() : null;
            String summary = node.has("summary") ? node.get("summary").asText() : null;
            Integer estMinutes = node.has("estMinutes") ? node.get("estMinutes").asInt() : null;
            return new ContentAnalysis(tags, difficulty, summary, estMinutes);
        } catch (Exception e) {
            throw new IllegalStateException("Claude 분석 응답 파싱 실패: " + text, e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
