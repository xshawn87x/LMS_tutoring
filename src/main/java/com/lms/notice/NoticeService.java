package com.lms.notice;

import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import com.lms.notice.dto.NoticeDtos.NoticeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 공지 CRUD. 학원 공지(ACADEMY)와 강의 공지(COURSE, 과정 소속 확인). RLS로 테넌트 격리.
 * 작성/수정/삭제는 컨트롤러에서 INSTRUCTOR/ADMIN으로 제한한다.
 */
@Service
@Transactional
public class NoticeService {

    private final NoticeRepository repository;
    private final CourseService courseService;

    public NoticeService(NoticeRepository repository, CourseService courseService) {
        this.repository = repository;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public List<Notice> listAcademy() {
        return repository.findByScopeOrderByPinnedDescCreatedAtDesc(NoticeScope.ACADEMY);
    }

    @Transactional(readOnly = true)
    public List<Notice> listCourse(UUID courseId) {
        courseService.requireExists(courseId);
        return repository.findByCourseIdOrderByPinnedDescCreatedAtDesc(courseId);
    }

    public Notice createAcademy(NoticeRequest req, String author) {
        return repository.save(new Notice(NoticeScope.ACADEMY, null, req.title(), req.body(), author, req.pinned()));
    }

    public Notice createCourse(UUID courseId, NoticeRequest req, String author) {
        courseService.requireExists(courseId);
        return repository.save(new Notice(NoticeScope.COURSE, courseId, req.title(), req.body(), author, req.pinned()));
    }

    public Notice update(UUID id, NoticeRequest req) {
        Notice notice = require(id);
        notice.update(req.title(), req.body(), req.pinned());
        return notice;
    }

    public void delete(UUID id) {
        repository.delete(require(id));
    }

    private Notice require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("공지를 찾을 수 없습니다: " + id));
    }
}
