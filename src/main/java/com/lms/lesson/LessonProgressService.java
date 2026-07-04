package com.lms.lesson;

import com.lms.certificate.CertificateService;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.lesson.dto.LessonProgressRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 학습창 진도 처리: 레슨 재생 위치(이어듣기) 저장 + 완료 시 수강 진도 자동 재계산 + 수료 연동.
 * 진도율은 (완료 레슨 수 / 전체 레슨 수) × 100 으로 서버가 계산한다.
 */
@Service
@Transactional
public class LessonProgressService {

    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FeatureService featureService;
    private final CertificateService certificateService;

    public LessonProgressService(LessonProgressRepository progressRepository, LessonRepository lessonRepository,
                                 EnrollmentRepository enrollmentRepository, FeatureService featureService,
                                 CertificateService certificateService) {
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.featureService = featureService;
        this.certificateService = certificateService;
    }

    public LessonProgress save(UUID lessonId, String studentId, LessonProgressRequest request) {
        featureService.requireEnabled(Feature.LESSONS);

        // RLS가 타 테넌트 레슨을 숨긴다 — 없으면 404
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("레슨을 찾을 수 없습니다"));
        UUID courseId = lesson.getCourseId();

        // 수강 신청이 선행되어야 진도를 남긴다
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new NotFoundException("수강 신청이 필요합니다"));

        LessonProgress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> new LessonProgress(studentId, lessonId, courseId));
        progress.apply(request.lastPositionSeconds(), request.completed());
        progressRepository.save(progress);

        recomputeEnrollmentProgress(enrollment, courseId, studentId);
        return progress;
    }

    @Transactional(readOnly = true)
    public List<LessonProgress> listForCourse(UUID courseId, String studentId) {
        featureService.requireEnabled(Feature.LESSONS);
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId);
    }

    private void recomputeEnrollmentProgress(Enrollment enrollment, UUID courseId, String studentId) {
        long total = lessonRepository.countByCourseId(courseId);
        long completed = progressRepository.countByStudentIdAndCourseIdAndCompletedTrue(studentId, courseId);
        int pct = (total > 0) ? (int) Math.round(completed * 100.0 / total) : 0;
        enrollment.updateProgress(pct);
        // 진도 100% 도달 시 수료 조건 확인 (모든 퀴즈 통과 등) → 충족 시 수료증 발급
        if (pct >= 100) {
            certificateService.checkAndIssue(courseId, studentId);
        }
    }
}
