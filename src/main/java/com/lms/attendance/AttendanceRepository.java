package com.lms.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByGroupIdAndAttDate(UUID groupId, LocalDate attDate);

    List<Attendance> findByStudentSubjectOrderByAttDateDesc(String studentSubject);

    Optional<Attendance> findByGroupIdAndStudentSubjectAndAttDate(UUID groupId, String studentSubject, LocalDate attDate);
}
