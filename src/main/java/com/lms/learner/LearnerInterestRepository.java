package com.lms.learner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearnerInterestRepository extends JpaRepository<LearnerInterest, UUID> {
    List<LearnerInterest> findByStudentId(String studentId);
    void deleteByStudentId(String studentId);
}
