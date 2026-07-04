package com.lms.guardian;

import com.lms.attendance.Attendance;
import com.lms.attendance.AttendanceService;
import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.counseling.CounselingRecord;
import com.lms.counseling.CounselingService;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentService;
import com.lms.error.BadRequestException;
import com.lms.error.ConflictException;
import com.lms.error.ForbiddenException;
import com.lms.error.NotFoundException;
import com.lms.security.Roles;
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
    private final AppUserRepository userRepository;

    public GuardianService(GuardianLinkRepository repository, EnrollmentService enrollmentService,
                           AttendanceService attendanceService, CounselingService counselingService,
                           AppUserRepository userRepository) {
        this.repository = repository;
        this.enrollmentService = enrollmentService;
        this.attendanceService = attendanceService;
        this.counselingService = counselingService;
        this.userRepository = userRepository;
    }

    /** 자녀 요약(이메일 + 표시이름). 학부모 화면에서 이름으로 보이도록. */
    public record ChildInfo(String subject, String displayName) {}

    // --- 학부모 ---

    @Transactional(readOnly = true)
    public List<String> childrenOf(String parentSubject) {
        return repository.findByParentSubject(parentSubject.trim().toLowerCase()).stream()
                .map(GuardianLink::getStudentSubject)
                .toList();
    }

    /** 연결된 자녀 목록을 표시이름과 함께 반환. */
    @Transactional(readOnly = true)
    public List<ChildInfo> childrenDetailedOf(String parentSubject) {
        return repository.findByParentSubject(parentSubject.trim().toLowerCase()).stream()
                .map(l -> {
                    String s = l.getStudentSubject();
                    String name = userRepository.findByEmail(s).map(AppUser::getDisplayName).orElse(null);
                    return new ChildInfo(s, name);
                })
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
        // 실제 존재하는 계정끼리, 역할이 맞을 때만 연결한다(오타·죽은 연결 방지).
        requireRole(parent, Roles.PARENT, "학부모(PARENT)");
        requireRole(student, Roles.STUDENT, "학생(STUDENT)");
        if (parent.equals(student)) {
            throw new BadRequestException("본인을 자녀로 연결할 수 없습니다");
        }
        if (repository.existsByParentSubjectAndStudentSubject(parent, student)) {
            throw new ConflictException("이미 연결되어 있습니다");
        }
        return repository.save(new GuardianLink(parent, student));
    }

    /** 해당 이메일 계정이 현재 테넌트에 존재하고 지정 역할을 갖는지 확인. */
    private void requireRole(String email, String role, String label) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(label + " 계정을 찾을 수 없습니다: " + email));
        if (!user.roleList().contains(role)) {
            throw new BadRequestException(label + " 역할을 가진 계정이 아닙니다: " + email);
        }
    }

    public void unlink(UUID id) {
        GuardianLink link = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("연결을 찾을 수 없습니다: " + id));
        repository.delete(link);
    }
}
