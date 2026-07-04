package com.lms.attendance.dto;

import com.lms.attendance.Attendance;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record Entry(String studentSubject, String status, String note) {
    }

    /** 한 반의 특정 날짜 출석 일괄 기록. */
    public record MarkRequest(@NotNull LocalDate date, @NotEmpty List<Entry> entries) {
    }

    public record AttendanceResponse(
            UUID id, UUID groupId, String studentSubject, LocalDate attDate, String status, String note) {
        public static AttendanceResponse from(Attendance a) {
            return new AttendanceResponse(a.getId(), a.getGroupId(), a.getStudentSubject(),
                    a.getAttDate(), a.getStatus().name(), a.getNote());
        }
    }
}
