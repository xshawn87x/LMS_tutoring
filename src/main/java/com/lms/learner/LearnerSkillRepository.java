package com.lms.learner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LearnerSkillRepository extends JpaRepository<LearnerSkill, UUID> {
    List<LearnerSkill> findByStudentId(String studentId);
    void deleteByStudentId(String studentId);
}
