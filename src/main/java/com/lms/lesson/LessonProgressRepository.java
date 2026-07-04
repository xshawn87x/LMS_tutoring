package com.lms.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    // tenant_id 필터는 없다 — RLS가 현재 테넌트로 자동 격리.
    Optional<LessonProgress> findByStudentIdAndLessonId(String studentId, UUID lessonId);

    List<LessonProgress> findByStudentIdAndCourseId(String studentId, UUID courseId);

    long countByStudentIdAndCourseIdAndCompletedTrue(String studentId, UUID courseId);

    // 수강 취소 시 해당 과정의 내 진도 기록 초기화
    void deleteByStudentIdAndCourseId(String studentId, UUID courseId);
}
