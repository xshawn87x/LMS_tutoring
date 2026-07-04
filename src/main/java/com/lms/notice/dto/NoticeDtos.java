package com.lms.notice.dto;

import com.lms.notice.Notice;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NoticeDtos {

    private NoticeDtos() {
    }

    public record NoticeRequest(
            @NotBlank String title,
            String body,
            boolean pinned) {
    }

    public record NoticeResponse(
            UUID id,
            String scope,
            UUID courseId,
            String title,
            String body,
            String author,
            boolean pinned,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        public static NoticeResponse from(Notice n) {
            return new NoticeResponse(n.getId(), n.getScope().name(), n.getCourseId(),
                    n.getTitle(), n.getBody(), n.getAuthor(), n.isPinned(), n.getCreatedAt(), n.getUpdatedAt());
        }
    }
}
