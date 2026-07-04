package com.lms.attendance;

import com.lms.attendance.dto.AttendanceDtos.AttendanceResponse;
import com.lms.attendance.dto.AttendanceDtos.MarkRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 출석 API. 기록/반별 조회=INSTRUCTOR/ADMIN, 내 출석=학생 본인. */
@RestController
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/api/groups/{groupId}/attendance")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<AttendanceResponse> mark(@PathVariable UUID groupId, @Valid @RequestBody MarkRequest request) {
        return service.mark(groupId, request.date(), request.entries()).stream()
                .map(AttendanceResponse::from).toList();
    }

    @GetMapping("/api/groups/{groupId}/attendance")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<AttendanceResponse> forDate(@PathVariable UUID groupId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forGroupDate(groupId, date).stream().map(AttendanceResponse::from).toList();
    }

    /** 학생 본인의 출석 이력. */
    @GetMapping("/api/me/attendance")
    public List<AttendanceResponse> myAttendance(@AuthenticationPrincipal Jwt jwt) {
        return service.forStudent(jwt.getSubject()).stream().map(AttendanceResponse::from).toList();
    }
}
