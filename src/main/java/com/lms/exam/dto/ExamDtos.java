package com.lms.exam.dto;

import com.lms.exam.Exam;
import com.lms.exam.ExamScore;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ExamDtos {

    private ExamDtos() {
    }

    /** 시험 생성/수정 요청. */
    public record ExamRequest(String title, String subject, LocalDate examDate, Integer maxScore, UUID groupId) {
    }

    /** 성적 입력 한 건. */
    public record ScoreEntry(String studentSubject, int score, String comment) {
    }

    /** 한 시험의 학생별 점수 일괄 입력. */
    public record RecordScoresRequest(List<ScoreEntry> entries) {
    }

    public record ExamResponse(UUID id, String title, String subject, LocalDate examDate, int maxScore, UUID groupId) {
        public static ExamResponse from(Exam e) {
            return new ExamResponse(e.getId(), e.getTitle(), e.getSubject(), e.getExamDate(), e.getMaxScore(), e.getGroupId());
        }
    }

    public record ScoreResponse(UUID id, UUID examId, String studentSubject, int score, String comment) {
        public static ScoreResponse from(ExamScore s) {
            return new ScoreResponse(s.getId(), s.getExamId(), s.getStudentSubject(), s.getScore(), s.getComment());
        }
    }

    /** 학생 성적 한 건(추이 그래프용) — 시험 정보 + 점수 + 백분율 + 석차/백분위. */
    public record StudentScore(
            UUID examId, String title, String subject, LocalDate examDate,
            int score, int maxScore, int percent, String comment,
            int rank, int totalTakers, int topPercent) {
    }

    /** 시험 석차표 한 줄 (강사용). */
    public record ExamRanking(
            String studentSubject, String studentName, int score, int percent,
            int rank, int totalTakers, int topPercent) {
    }
}
