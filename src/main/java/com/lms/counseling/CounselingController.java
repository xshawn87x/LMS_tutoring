package com.lms.counseling;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 상담 API. 기록=INSTRUCTOR/ADMIN, 예약 신청=인증자, 예약 상태변경=INSTRUCTOR/ADMIN. */
@RestController
public class CounselingController {

    private final CounselingService service;

    public CounselingController(CounselingService service) {
        this.service = service;
    }

    // --- 상담 기록 ---

    @PostMapping("/api/counseling/records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public RecordResponse addRecord(@RequestBody RecordRequest req, @AuthenticationPrincipal Jwt jwt) {
        return RecordResponse.from(service.addRecord(req.studentSubject(), jwt.getSubject(), req.content()));
    }

    @GetMapping("/api/counseling/records")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<RecordResponse> records(@RequestParam String student) {
        return service.recordsFor(student).stream().map(RecordResponse::from).toList();
    }

    /** 학생 본인의 상담 기록. */
    @GetMapping("/api/me/counseling")
    public List<RecordResponse> myRecords(@AuthenticationPrincipal Jwt jwt) {
        return service.recordsFor(jwt.getSubject()).stream().map(RecordResponse::from).toList();
    }

    // --- 상담 예약 ---

    @PostMapping("/api/counseling/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse request(@RequestBody AppointmentRequest req, @AuthenticationPrincipal Jwt jwt) {
        String student = (req.studentSubject() == null || req.studentSubject().isBlank())
                ? jwt.getSubject() : req.studentSubject();
        return AppointmentResponse.from(service.requestAppointment(student, jwt.getSubject(), req.preferredAt(), req.memo()));
    }

    @GetMapping("/api/counseling/appointments")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<AppointmentResponse> all() {
        return service.allAppointments().stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/api/me/appointments")
    public List<AppointmentResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.myAppointments(jwt.getSubject()).stream().map(AppointmentResponse::from).toList();
    }

    @PutMapping("/api/counseling/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public AppointmentResponse setStatus(@PathVariable UUID id, @RequestParam String status) {
        return AppointmentResponse.from(service.setStatus(id, status));
    }

    // --- DTO ---

    public record RecordRequest(@NotBlank String studentSubject, @NotBlank String content) {
    }

    public record RecordResponse(UUID id, String studentSubject, String counselor, String content, OffsetDateTime createdAt) {
        static RecordResponse from(CounselingRecord r) {
            return new RecordResponse(r.getId(), r.getStudentSubject(), r.getCounselor(), r.getContent(), r.getCreatedAt());
        }
    }

    public record AppointmentRequest(String studentSubject, OffsetDateTime preferredAt, String memo) {
    }

    public record AppointmentResponse(UUID id, String studentSubject, String requestedBy,
                                      OffsetDateTime preferredAt, String status, String memo, OffsetDateTime createdAt) {
        static AppointmentResponse from(CounselingAppointment a) {
            return new AppointmentResponse(a.getId(), a.getStudentSubject(), a.getRequestedBy(),
                    a.getPreferredAt(), a.getStatus(), a.getMemo(), a.getCreatedAt());
        }
    }
}
