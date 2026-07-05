package com.lms.attendance;

import com.lms.attendance.dto.AttendanceDtos.Entry;
import com.lms.auth.AppUser;
import com.lms.auth.AppUserRepository;
import com.lms.error.BadRequestException;
import com.lms.group.GroupService;
import com.lms.guardian.GuardianLink;
import com.lms.guardian.GuardianLinkRepository;
import com.lms.notification.NotificationChannel;
import com.lms.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 출석 기록/조회. 반 존재 확인은 GroupService에 위임. 결석 시 학부모 자동 알림. RLS로 테넌트 격리. */
@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository repository;
    private final GroupService groupService;
    private final GuardianLinkRepository guardianLinkRepository;
    private final NotificationService notificationService;
    private final AppUserRepository userRepository;

    public AttendanceService(AttendanceRepository repository, GroupService groupService,
                             GuardianLinkRepository guardianLinkRepository,
                             NotificationService notificationService, AppUserRepository userRepository) {
        this.repository = repository;
        this.groupService = groupService;
        this.guardianLinkRepository = guardianLinkRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /** 한 반의 특정 날짜 출석을 일괄 기록(upsert). 새로 결석 처리되면 학부모에게 알림. */
    public List<Attendance> mark(UUID groupId, LocalDate date, List<Entry> entries) {
        groupService.get(groupId);   // 존재/테넌트 확인
        return entries.stream().map(e -> {
            AttendanceStatus status = parse(e.status());
            String student = e.studentSubject().trim().toLowerCase();
            Optional<Attendance> existing = repository.findByGroupIdAndStudentSubjectAndAttDate(groupId, student, date);
            boolean wasAbsent = existing.map(a -> a.getStatus() == AttendanceStatus.ABSENT).orElse(false);
            Attendance a = existing
                    .map(ex -> { ex.update(status, e.note()); return ex; })
                    .orElseGet(() -> repository.save(new Attendance(groupId, student, date, status, e.note())));
            // 새로 결석으로 바뀐 경우에만 알림(중복 알림 방지)
            if (status == AttendanceStatus.ABSENT && !wasAbsent) {
                notifyAbsence(student, date);
            }
            return a;
        }).toList();
    }

    /** 자녀 결석 시 연결된 학부모에게 인앱+이메일 알림. */
    private void notifyAbsence(String student, LocalDate date) {
        String name = userRepository.findByEmail(student).map(AppUser::getDisplayName).orElse(student);
        List<String> parents = guardianLinkRepository.findByStudentSubject(student)
                .stream().map(GuardianLink::getParentSubject).distinct().toList();
        String title = "자녀 결석 알림";
        String body = (name != null ? name : student) + " 학생이 " + date + " 결석 처리되었습니다.";
        for (String parent : parents) {
            notificationService.notify(parent, title, body);                                   // 인앱
            notificationService.dispatch(parent, title, body, NotificationChannel.EMAIL);       // 이메일(설정 시)
        }
    }

    @Transactional(readOnly = true)
    public List<Attendance> forGroupDate(UUID groupId, LocalDate date) {
        groupService.get(groupId);
        return repository.findByGroupIdAndAttDate(groupId, date);
    }

    @Transactional(readOnly = true)
    public List<Attendance> forStudent(String studentSubject) {
        return repository.findByStudentSubjectOrderByAttDateDesc(studentSubject.trim().toLowerCase());
    }

    private AttendanceStatus parse(String s) {
        try {
            return AttendanceStatus.valueOf(s.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("알 수 없는 출석 상태입니다: " + s);
        }
    }
}
