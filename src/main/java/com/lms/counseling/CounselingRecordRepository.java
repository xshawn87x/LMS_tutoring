package com.lms.counseling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CounselingRecordRepository extends JpaRepository<CounselingRecord, UUID> {
    List<CounselingRecord> findByStudentSubjectOrderByCreatedAtDesc(String studentSubject);
}
