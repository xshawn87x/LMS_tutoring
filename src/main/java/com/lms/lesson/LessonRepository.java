package com.lms.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    // tenant_id 필터는 없다 — RLS가 현재 테넌트로 자동 격리. course_id로만 좁힌다.
    List<Lesson> findByCourseIdOrderByOrderNoAsc(UUID courseId);

    long countByCourseId(UUID courseId);
}
