package com.lms.report.dto;

import com.lms.exam.dto.ExamDtos.StudentScore;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** 학부모 리포트 — 자녀의 성적·출석·과제·진도를 한 장으로 요약. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record AttendanceSummary(int present, int absent, int late, int excused, int total, int attendanceRate) {
    }

    /** 최근 출석 기록 한 건. */
    public record AttendanceEntry(LocalDate date, String status, String note) {
    }

    public record AssignmentSummary(int submitted, int graded, Integer avgScore) {
    }

    public record CourseSummary(int enrolled, int completed, int avgProgress) {
    }

    public record StudentReport(
            String studentSubject,
            String studentName,
            List<StudentScore> scores,
            Integer scoreAvgPercent,
            Integer latestPercent,
            AttendanceSummary attendance,
            List<AttendanceEntry> recentAttendance,
            AssignmentSummary assignments,
            CourseSummary courses,
            OffsetDateTime generatedAt) {
    }
}
