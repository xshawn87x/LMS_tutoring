package com.lms.report;

import com.lms.guardian.GuardianLink;
import com.lms.guardian.GuardianLinkRepository;
import com.lms.notification.NotificationChannel;
import com.lms.notification.NotificationService;
import com.lms.report.dto.ReportDtos.StudentReport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학부모 리포트 — 강사/관리자 조회 + 연결된 학부모에게 발송(인앱 알림).
 * 학부모 본인 조회는 GuardianController(/api/me/children/{student}/report).
 */
@RestController
public class ReportController {

    private final ReportService reportService;
    private final GuardianLinkRepository guardianLinkRepository;
    private final NotificationService notificationService;

    public ReportController(ReportService reportService, GuardianLinkRepository guardianLinkRepository,
                            NotificationService notificationService) {
        this.reportService = reportService;
        this.guardianLinkRepository = guardianLinkRepository;
        this.notificationService = notificationService;
    }

    /** 학생 리포트 조회 (강사/관리자). */
    @GetMapping("/api/students/{student}/report")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public StudentReport report(@PathVariable String student) {
        return reportService.buildReport(student);
    }

    /** 리포트를 연결된 학부모에게 발송 — 인앱 알림 + 이메일(SMTP 설정 시 실전송, 미설정 시 SIMULATED). */
    @PostMapping("/api/students/{student}/report/send")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public SendResult send(@PathVariable String student) {
        StudentReport report = reportService.buildReport(student);
        List<String> parents = guardianLinkRepository.findByStudentSubject(student.trim().toLowerCase())
                .stream().map(GuardianLink::getParentSubject).distinct().toList();
        String title = "자녀 성적 리포트가 도착했습니다";
        String body = reportService.summaryText(report);
        String emailStatus = parents.isEmpty() ? "NONE" : "SIMULATED";
        for (String parent : parents) {
            notificationService.notify(parent, title, body);                                  // 인앱(항상 동작)
            emailStatus = notificationService.dispatch(parent, title, body, NotificationChannel.EMAIL); // 이메일
        }
        return new SendResult(parents.size(), parents, emailStatus);
    }

    /** emailStatus: SENT(실발송) | SIMULATED(SMTP 미설정) | FAILED | NONE(대상 없음) */
    public record SendResult(int sent, List<String> parents, String emailStatus) {
    }
}
