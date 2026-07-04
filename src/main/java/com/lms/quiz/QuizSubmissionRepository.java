package com.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, UUID> {
    List<QuizSubmission> findByStudentIdOrderBySubmittedAtDesc(String studentId);

    List<QuizSubmission> findByQuizIdIn(List<UUID> quizIds);
}
