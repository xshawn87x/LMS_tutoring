package com.lms.course;

import com.lms.course.dto.CourseRequest;
import com.lms.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CourseService {

    private final CourseRepository repository;

    public CourseService(CourseRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return repository.findAll();
    }

    /** 공개된 과정만 (학생 카탈로그). */
    @Transactional(readOnly = true)
    public List<Course> findVisible() {
        return repository.findByPublishedTrue();
    }

    /** 노출 여부 토글 (강사/관리자). */
    public Course setPublished(UUID id, boolean published) {
        Course course = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + id));
        course.setPublished(published);
        return course;
    }

    /** 수강료 설정 (강사/관리자). */
    public Course setTuition(UUID id, int fee) {
        Course course = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + id));
        course.setTuitionFee(fee);
        return course;
    }

    /** 결제 등 하위 모듈이 수강료를 읽을 때 쓰는 가드 겸 조회. */
    @Transactional(readOnly = true)
    public Course require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(UUID id) {
        return repository.findById(id);
    }

    public Course create(CourseRequest request) {
        // tenant_id는 Course(TenantOwned)의 @PrePersist가 현재 테넌트로 자동 설정한다.
        return repository.save(
                new Course(request.title(), request.description(), request.categoryCode(), request.level()));
    }

    public Course update(UUID id, CourseRequest request) {
        // RLS가 타 테넌트 과정을 숨긴다 — 없으면 404
        Course course = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + id));
        course.update(request.title(), request.description(), request.categoryCode(), request.level());
        return course;
    }

    public void delete(UUID id) {
        Course course = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("과정을 찾을 수 없습니다: " + id));
        // 레슨·수강·퀴즈·진도·수료증은 FK ON DELETE CASCADE로 함께 삭제된다.
        repository.delete(course);
    }

    /**
     * 과정이 현재 테넌트에 존재하는지 보장한다 (없으면 404).
     * RLS가 다른 테넌트의 과정을 숨기므로, 타 테넌트/없는 과정에 하위 자원(레슨·수강·퀴즈)을
     * 붙이려는 시도는 자연히 404가 된다. 하위 모듈들이 공유하는 가드.
     */
    @Transactional(readOnly = true)
    public void requireExists(UUID courseId) {
        if (!repository.existsById(courseId)) {
            throw new NotFoundException("과정을 찾을 수 없습니다: " + courseId);
        }
    }
}
