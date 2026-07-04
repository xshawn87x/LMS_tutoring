package com.lms.assignment.dto;

import com.lms.assignment.Assignment;
import com.lms.assignment.AssignmentSubmission;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AssignmentDtos {

    private AssignmentDtos() {
    }

    public record AssignmentRequest(
            @NotBlank String title, String description, OffsetDateTime dueAt, int maxScore) {
    }

    public record SubmitRequest(String textAnswer, String fileUrl) {
    }

    public record GradeRequest(int score, String feedback) {
    }

    public record AssignmentResponse(
            UUID id, UUID courseId, String title, String description,
            OffsetDateTime dueAt, int maxScore, OffsetDateTime createdAt) {
        public static AssignmentResponse from(Assignment a) {
            return new AssignmentResponse(a.getId(), a.getCourseId(), a.getTitle(), a.getDescription(),
                    a.getDueAt(), a.getMaxScore(), a.getCreatedAt());
        }
    }

    public record SubmissionResponse(
            UUID id, UUID assignmentId, String student, String textAnswer, String fileUrl,
            OffsetDateTime submittedAt, Integer score, String feedback, OffsetDateTime gradedAt) {
        public static SubmissionResponse from(AssignmentSubmission s) {
            return new SubmissionResponse(s.getId(), s.getAssignmentId(), s.getStudent(), s.getTextAnswer(),
                    s.getFileUrl(), s.getSubmittedAt(), s.getScore(), s.getFeedback(), s.getGradedAt());
        }
    }
}
