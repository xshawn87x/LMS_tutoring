package com.lms.curation.dto;

import com.lms.curation.ContentInsight;

import java.time.OffsetDateTime;
import java.util.List;

public record ContentInsightResponse(
        List<String> tags,
        Integer difficulty,
        String summary,
        Integer estMinutes,
        String generatedBy,
        OffsetDateTime updatedAt
) {
    public static ContentInsightResponse from(ContentInsight insight) {
        return new ContentInsightResponse(
                insight.getTags(),
                insight.getDifficulty(),
                insight.getSummary(),
                insight.getEstMinutes(),
                insight.getGeneratedBy(),
                insight.getUpdatedAt());
    }
}
