package com.lms.lesson;

import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.lesson.dto.LessonRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseService courseService;
    private final FeatureService featureService;

    public LessonService(LessonRepository lessonRepository, CourseService courseService,
                         FeatureService featureService) {
        this.lessonRepository = lessonRepository;
        this.courseService = courseService;
        this.featureService = featureService;
    }

    public Lesson add(UUID courseId, LessonRequest request) {
        featureService.requireEnabled(Feature.LESSONS);
        courseService.requireExists(courseId);
        return lessonRepository.save(
                new Lesson(courseId, request.title(), request.content(), request.videoUrl(), request.orderNo()));
    }

    @Transactional(readOnly = true)
    public List<Lesson> listByCourse(UUID courseId) {
        featureService.requireEnabled(Feature.LESSONS);
        courseService.requireExists(courseId);
        return lessonRepository.findByCourseIdOrderByOrderNoAsc(courseId);
    }

    public Lesson update(UUID courseId, UUID lessonId, LessonRequest request) {
        featureService.requireEnabled(Feature.LESSONS);
        Lesson lesson = requireLessonInCourse(courseId, lessonId);
        lesson.update(request.title(), request.content(), request.videoUrl(), request.orderNo());
        return lesson;
    }

    public void delete(UUID courseId, UUID lessonId) {
        featureService.requireEnabled(Feature.LESSONS);
        Lesson lesson = requireLessonInCourse(courseId, lessonId);
        lessonRepository.delete(lesson);
    }

    /** 레슨이 현재 테넌트의 해당 과정에 속하는지 확인 (RLS + course 일치). 없으면 404. */
    private Lesson requireLessonInCourse(UUID courseId, UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .filter(l -> l.getCourseId().equals(courseId))
                .orElseThrow(() -> new NotFoundException("레슨을 찾을 수 없습니다"));
    }
}
