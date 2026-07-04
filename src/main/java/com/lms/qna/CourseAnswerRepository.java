package com.lms.qna;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseAnswerRepository extends JpaRepository<CourseAnswer, UUID> {
    List<CourseAnswer> findByQuestionIdOrderByCreatedAtAsc(UUID questionId);

    List<CourseAnswer> findByQuestionIdIn(Collection<UUID> questionIds);
}
