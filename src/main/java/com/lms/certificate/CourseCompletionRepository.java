package com.lms.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, UUID> {
    Optional<CourseCompletion> findByCourseIdAndStudentId(UUID courseId, String studentId);
    List<CourseCompletion> findByStudentIdOrderByIssuedAtDesc(String studentId);
    long countByCourseId(UUID courseId);
}
