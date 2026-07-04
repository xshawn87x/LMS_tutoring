package com.lms.counseling;

import com.lms.error.BadRequestException;
import com.lms.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 상담 기록 + 예약. RLS로 테넌트 격리. 권한은 컨트롤러에서 강제. */
@Service
@Transactional
public class CounselingService {

    private static final Set<String> APPT_STATUS = Set.of("REQUESTED", "CONFIRMED", "CANCELLED");

    private final CounselingRecordRepository recordRepository;
    private final CounselingAppointmentRepository appointmentRepository;

    public CounselingService(CounselingRecordRepository recordRepository,
                             CounselingAppointmentRepository appointmentRepository) {
        this.recordRepository = recordRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // --- 상담 기록 ---

    public CounselingRecord addRecord(String studentSubject, String counselor, String content) {
        return recordRepository.save(new CounselingRecord(studentSubject.trim().toLowerCase(), counselor, content));
    }

    @Transactional(readOnly = true)
    public List<CounselingRecord> recordsFor(String studentSubject) {
        return recordRepository.findByStudentSubjectOrderByCreatedAtDesc(studentSubject.trim().toLowerCase());
    }

    // --- 상담 예약 ---

    public CounselingAppointment requestAppointment(String studentSubject, String requestedBy,
                                                    OffsetDateTime preferredAt, String memo) {
        return appointmentRepository.save(new CounselingAppointment(
                studentSubject.trim().toLowerCase(), requestedBy, preferredAt, memo));
    }

    @Transactional(readOnly = true)
    public List<CounselingAppointment> allAppointments() {
        return appointmentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<CounselingAppointment> myAppointments(String requestedBy) {
        return appointmentRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy);
    }

    public CounselingAppointment setStatus(UUID id, String status) {
        String s = status.trim().toUpperCase();
        if (!APPT_STATUS.contains(s)) {
            throw new BadRequestException("알 수 없는 상태입니다: " + status);
        }
        CounselingAppointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다: " + id));
        appt.setStatus(s);
        return appt;
    }
}
