package com.lms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    // tenant_id 필터 없음 — RLS가 현재 테넌트로 자동 격리.
    List<Exam> findAllByOrderByExamDateDesc();
}
