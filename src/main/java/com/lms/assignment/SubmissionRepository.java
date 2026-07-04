package com.lms.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {
    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudent(UUID assignmentId, String student);

    // 한 학생의 모든 과제 제출물(리포트 과제 요약용).
    List<AssignmentSubmission> findByStudent(String student);
}
