package com.lms.attendance;

import com.lms.attendance.dto.AttendanceDtos.Entry;
import com.lms.error.BadRequestException;
import com.lms.group.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 출석 기록/조회. 반 존재 확인은 GroupService에 위임. RLS로 테넌트 격리. */
@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository repository;
    private final GroupService groupService;

    public AttendanceService(AttendanceRepository repository, GroupService groupService) {
        this.repository = repository;
        this.groupService = groupService;
    }

    /** 한 반의 특정 날짜 출석을 일괄 기록(upsert). */
    public List<Attendance> mark(UUID groupId, LocalDate date, List<Entry> entries) {
        groupService.get(groupId);   // 존재/테넌트 확인
        return entries.stream().map(e -> {
            AttendanceStatus status = parse(e.status());
            String student = e.studentSubject().trim().toLowerCase();
            Attendance a = repository.findByGroupIdAndStudentSubjectAndAttDate(groupId, student, date)
                    .map(existing -> { existing.update(status, e.note()); return existing; })
                    .orElseGet(() -> repository.save(new Attendance(groupId, student, date, status, e.note())));
            return a;
        }).toList();
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
