package com.lms.exam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamScoreRepository extends JpaRepository<ExamScore, UUID> {

    List<ExamScore> findByExamId(UUID examId);

    List<ExamScore> findByStudentSubject(String studentSubject);

    Optional<ExamScore> findByExamIdAndStudentSubject(UUID examId, String studentSubject);
}
