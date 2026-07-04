package com.lms.tuition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentPaymentRepository extends JpaRepository<StudentPayment, UUID> {
    List<StudentPayment> findByStudentSubjectOrderByCreatedAtDesc(String studentSubject);

    List<StudentPayment> findAllByOrderByCreatedAtDesc();

    boolean existsByStudentSubjectAndCourseIdAndStatus(String studentSubject, UUID courseId, String status);
}
