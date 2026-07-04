package com.lms.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    // tenant_id 필터는 없다 — RLS가 현재 테넌트로 자동 격리.
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(String studentId);

    boolean existsByCourseIdAndStudentId(UUID courseId, String studentId);

    Optional<Enrollment> findByCourseIdAndStudentId(UUID courseId, String studentId);

    List<Enrollment> findByCourseId(UUID courseId);
}
