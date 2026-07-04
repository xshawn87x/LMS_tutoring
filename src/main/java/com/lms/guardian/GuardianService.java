package com.lms.guardian;

import com.lms.attendance.Attendance;
import com.lms.attendance.AttendanceService;
import com.lms.counseling.CounselingRecord;
import com.lms.counseling.CounselingService;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
import com.lms.error.ConflictException;
import com.lms.error.ForbiddenException;
import com.lms.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 학부모-자녀 연결 관리 + 학부모의 자녀 학습 현황 조회.
 * 연결(link/unlink)은 학원 관리자, 조회는 학부모 본인(연결된 자녀만). RLS로 테넌트 격리.
 */
@Service
@Transactional
public class GuardianService {

    private final GuardianLinkRepository repository;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final CounselingService counselingService;

    public GuardianService(GuardianLinkRepository repository, EnrollmentService enrollmentService,
                           AttendanceService attendanceService, CounselingService counselingService) {
        this.repository = repository;
        this.enrollmentService = enrollmentService;
        this.attendanceService = attendanceService;
        this.counselingService = counselingService;
    }

    // --- 학부모 ---

    @Transactional(readOnly = true)
    public List<String> childrenOf(String parentSubject) {
        return repository.findByParentSubject(parentSubject.trim().toLowerCase()).stream()
                .map(GuardianLink::getStudentSubject)
                .toList();
    }

    /** 연결된 자녀의 수강 현황. 연결이 없으면 403. */
    @Transactional(readOnly = true)
    public List<Enrollment> childEnrollments(String parentSubject, String studentSubject) {
        String student = requireLinked(parentSubject, studentSubject);
        return enrollmentService.listMine(student);
    }

    /** 연결된 자녀의 출석 이력. 연결이 없으면 403. */
    @Transactional(readOnly = true)
    public List<Attendance> childAttendance(String parentSubject, String studentSubject) {
        String student = requireLinked(parentSubject, studentSubject);
        return attendanceService.forStudent(student);
    }

    /** 연결된 자녀의 상담 기록. 연결이 없으면 403. */
    @Transactional(readOnly = true)
    public List<CounselingRecord> childCounseling(String parentSubject, String studentSubject) {
        String student = requireLinked(parentSubject, studentSubject);
        return counselingService.recordsFor(student);
    }

    /** 부모-자녀 연결을 확인하고, 정규화된 자녀 subject를 반환. 연결 없으면 403. (대소문자 무관) */
    private String requireLinked(String parentSubject, String studentSubject) {
        String parent = parentSubject.trim().toLowerCase();
        String student = studentSubject.trim().toLowerCase();
        if (!repository.existsByParentSubjectAndStudentSubject(parent, student)) {
            throw new ForbiddenException("연결된 자녀가 아닙니다");
        }
        return student;
    }

    // --- 관리자 ---

    @Transactional(readOnly = true)
    public List<GuardianLink> allLinks() {
        return repository.findAll();
    }

    public GuardianLink link(String parentSubject, String studentSubject) {
        String parent = parentSubject.trim().toLowerCase();
        String student = studentSubject.trim().toLowerCase();
        if (repository.existsByParentSubjectAndStudentSubject(parent, student)) {
            throw new ConflictException("이미 연결되어 있습니다");
        }
        return repository.save(new GuardianLink(parent, student));
    }

    public void unlink(UUID id) {
        GuardianLink link = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("연결을 찾을 수 없습니다: " + id));
        repository.delete(link);
    }
}
