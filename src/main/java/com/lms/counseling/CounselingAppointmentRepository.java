package com.lms.counseling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CounselingAppointmentRepository extends JpaRepository<CounselingAppointment, UUID> {
    List<CounselingAppointment> findAllByOrderByCreatedAtDesc();

    List<CounselingAppointment> findByRequestedByOrderByCreatedAtDesc(String requestedBy);
}
