package com.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 주목: tenant_id로 필터링하는 메서드가 하나도 없다.
 * findAll() / findById()는 그냥 전체를 조회하는 듯 보이지만,
 * RLS가 현재 테넌트의 행만 돌려준다. 격리는 DB가 강제한다.
 */
public interface CourseRepository extends JpaRepository<Course, UUID> {

    /** 공개된 과정만 (학생 카탈로그용). */
    List<Course> findByPublishedTrue();
}
