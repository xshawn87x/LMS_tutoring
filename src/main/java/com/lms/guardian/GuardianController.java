package com.lms.guardian;

import com.lms.attendance.dto.AttendanceDtos.AttendanceResponse;
import com.lms.counseling.CounselingRecord;
import com.lms.enrollment.dto.EnrollmentResponse;
import com.lms.exam.dto.ExamDtos.StudentScore;
import com.lms.report.dto.ReportDtos.StudentReport;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 학부모 조회 API(연결된 자녀 학습현황) + 학원 관리자 연결 관리 API.
 */
@RestController
public class GuardianController {

    private final GuardianService service;

    public GuardianController(GuardianService service) {
        this.service = service;
    }

    // --- 학부모(PARENT) ---

    /** 내 자녀(연결된 학생) 목록 — 이메일 + 표시이름. */
    @GetMapping("/api/me/children")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public List<GuardianService.ChildInfo> myChildren(@AuthenticationPrincipal Jwt jwt) {
        return service.childrenDetailedOf(jwt.getSubject());
    }

    /** 자녀의 수강 현황(진도). 연결된 자녀만. */
    @GetMapping("/api/me/children/{studentSubject}/enrollments")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public List<EnrollmentResponse> childEnrollments(@PathVariable String studentSubject,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return service.childEnrollments(jwt.getSubject(), studentSubject).stream()
                .map(EnrollmentResponse::from).toList();
    }

    /** 자녀의 출석 이력. 연결된 자녀만. */
    @GetMapping("/api/me/children/{studentSubject}/attendance")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public List<AttendanceResponse> childAttendance(@PathVariable String studentSubject,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return service.childAttendance(jwt.getSubject(), studentSubject).stream()
                .map(AttendanceResponse::from).toList();
    }

    /** 자녀의 상담 기록. 연결된 자녀만. */
    @GetMapping("/api/me/children/{studentSubject}/counseling")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public List<ChildCounselingView> childCounseling(@PathVariable String studentSubject,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return service.childCounseling(jwt.getSubject(), studentSubject).stream()
                .map(ChildCounselingView::from).toList();
    }

    public record ChildCounselingView(String counselor, String content, java.time.OffsetDateTime createdAt) {
        static ChildCounselingView from(CounselingRecord r) {
            return new ChildCounselingView(r.getCounselor(), r.getContent(), r.getCreatedAt());
        }
    }

    /** 자녀의 성적 추이. 연결된 자녀만. */
    @GetMapping("/api/me/children/{studentSubject}/scores")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public List<StudentScore> childScores(@PathVariable String studentSubject,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.childScores(jwt.getSubject(), studentSubject);
    }

    /** 자녀의 종합 리포트(성적·출석·과제·진도). 연결된 자녀만. */
    @GetMapping("/api/me/children/{studentSubject}/report")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public StudentReport childReport(@PathVariable String studentSubject,
                                     @AuthenticationPrincipal Jwt jwt) {
        return service.childReport(jwt.getSubject(), studentSubject);
    }

    // --- 학원 관리자(ADMIN) ---

    @GetMapping("/api/admin/guardians")
    @PreAuthorize("hasRole('ADMIN')")
    public List<GuardianLinkView> links() {
        return service.allLinks().stream().map(GuardianLinkView::from).toList();
    }

    @PostMapping("/api/admin/guardians")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GuardianLinkView link(@RequestBody GuardianLinkRequest request) {
        return GuardianLinkView.from(service.link(request.parentSubject(), request.studentSubject()));
    }

    @DeleteMapping("/api/admin/guardians/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void unlink(@PathVariable UUID id) {
        service.unlink(id);
    }

    public record GuardianLinkRequest(@NotBlank String parentSubject, @NotBlank String studentSubject) {
    }

    public record GuardianLinkView(UUID id, String parentSubject, String studentSubject, OffsetDateTime createdAt) {
        static GuardianLinkView from(GuardianLink l) {
            return new GuardianLinkView(l.getId(), l.getParentSubject(), l.getStudentSubject(), l.getCreatedAt());
        }
    }
}
