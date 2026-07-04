package com.lms.qna;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, UUID> {
    List<CourseQuestion> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
