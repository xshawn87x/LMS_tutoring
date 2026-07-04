package com.lms.report;

import com.lms.assignment.AssignmentSubmission;
import com.lms.assignment.SubmissionRepository;
import com.lms.attendance.Attendance;
import com.lms.attendance.AttendanceService;
import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
import com.lms.enrollment.EnrollmentStatus;
import com.lms.exam.ExamService;
import com.lms.exam.dto.ExamDtos.StudentScore;
import com.lms.report.dto.ReportDtos.AssignmentSummary;
import com.lms.report.dto.ReportDtos.AttendanceEntry;
import com.lms.report.dto.ReportDtos.AttendanceSummary;
import com.lms.report.dto.ReportDtos.CourseSummary;
import com.lms.report.dto.ReportDtos.StudentReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 학부모 리포트 집계 — 자녀의 성적 추이·출석·과제·진도를 모아 한 장으로 만든다.
 * 여러 모듈(exam·attendance·assignment·enrollment)의 읽기 전용 집계라 순환 의존이 없다.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ExamService examService;
    private final AttendanceService attendanceService;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentService enrollmentService;
    private final AppUserRepository userRepository;

    public ReportService(ExamService examService, AttendanceService attendanceService,
                         SubmissionRepository submissionRepository, EnrollmentService enrollmentService,
                         AppUserRepository userRepository) {
        this.examService = examService;
        this.attendanceService = attendanceService;
        this.submissionRepository = submissionRepository;
        this.enrollmentService = enrollmentService;
        this.userRepository = userRepository;
    }

    public StudentReport buildReport(String rawStudent) {
        String student = rawStudent.trim().toLowerCase();
        String name = userRepository.findByEmail(student).map(AppUser::getDisplayName).orElse(null);

        List<StudentScore> scores = examService.studentScores(student);
        Integer scoreAvg = scores.isEmpty() ? null
                : (int) Math.round(scores.stream().mapToInt(StudentScore::percent).average().orElse(0));
        Integer latest = scores.isEmpty() ? null : scores.get(scores.size() - 1).percent(); // 시행일 오름차순

        List<Attendance> attList = attendanceService.forStudent(student); // 최신순
        AttendanceSummary att = summarizeAttendance(attList);
        List<AttendanceEntry> recent = attList.stream().limit(10)
                .map(a -> new AttendanceEntry(a.getAttDate(), a.getStatus().name(), a.getNote()))
                .toList();
        AssignmentSummary asg = summarizeAssignments(submissionRepository.findByStudent(student));
        CourseSummary crs = summarizeCourses(enrollmentService.listMine(student));

        return new StudentReport(student, name, scores, scoreAvg, latest, att, recent, asg, crs, OffsetDateTime.now());
    }

    /** 리포트 발송 알림에 담을 한 줄 요약. */
    public String summaryText(StudentReport r) {
        String who = r.studentName() != null ? r.studentName() : r.studentSubject();
        StringBuilder sb = new StringBuilder(who).append(" 학생 리포트 — ");
        if (r.scoreAvgPercent() != null) sb.append("성적 평균 ").append(r.scoreAvgPercent()).append("% · ");
        sb.append("출석률 ").append(r.attendance().attendanceRate()).append("% · ");
        sb.append("과제 ").append(r.assignments().submitted()).append("건 제출");
        return sb.toString();
    }

    private AttendanceSummary summarizeAttendance(List<Attendance> list) {
        int present = 0, absent = 0, late = 0, excused = 0;
        for (Attendance a : list) {
            switch (a.getStatus()) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case LATE -> late++;
                case EXCUSED -> excused++;
            }
        }
        int total = list.size();
        // 출석률 = (출석 + 지각 + 공결) / 전체 — 결석만 미차감
        int attended = present + late + excused;
        int rate = total == 0 ? 0 : (int) Math.round(attended * 100.0 / total);
        return new AttendanceSummary(present, absent, late, excused, total, rate);
    }

    private AssignmentSummary summarizeAssignments(List<AssignmentSubmission> subs) {
        int submitted = subs.size();
        List<Integer> graded = subs.stream().map(AssignmentSubmission::getScore).filter(s -> s != null).toList();
        Integer avg = graded.isEmpty() ? null
                : (int) Math.round(graded.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new AssignmentSummary(submitted, graded.size(), avg);
    }

    private CourseSummary summarizeCourses(List<Enrollment> enrollments) {
        int enrolled = enrollments.size();
        int completed = (int) enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();
        int avg = enrolled == 0 ? 0
                : (int) Math.round(enrollments.stream().mapToInt(Enrollment::getProgress).average().orElse(0));
        return new CourseSummary(enrolled, completed, avg);
    }
}
