package com.lms.enrollment;

import com.lms.certificate.CertificateService;
import com.lms.course.CourseService;
import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.lesson.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final FeatureService featureService;
    private final CertificateService certificateService;
    private final LessonProgressRepository lessonProgressRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseService courseService,
                             FeatureService featureService, CertificateService certificateService,
                             LessonProgressRepository lessonProgressRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.featureService = featureService;
        this.certificateService = certificateService;
        this.lessonProgressRepository = lessonProgressRepository;
    }

    public Enrollment enroll(UUID courseId, String studentId) {
        featureService.requireEnabled(Feature.ENROLLMENTS);
        courseService.requireExists(courseId);
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new ConflictException("이미 수강 중인 과정입니다");
        }
        return enrollmentRepository.save(new Enrollment(courseId, studentId));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> listMine(String studentId) {
        featureService.requireEnabled(Feature.ENROLLMENTS);
        return enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(studentId);
    }

    public Enrollment updateProgress(UUID enrollmentId, String studentId, int progress) {
        featureService.requireEnabled(Feature.ENROLLMENTS);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                // RLS가 타 테넌트 행을 숨기고, 본인 수강이 아니면 노출하지 않는다(404)
                .filter(e -> e.getStudentId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("수강 정보를 찾을 수 없습니다"));
        enrollment.updateProgress(progress);
        // 진도 100% 도달 시 수료 조건을 확인해 수료증 발급 (조건 미충족/기능 OFF면 아무 일도 안 함)
        if (enrollment.getProgress() >= 100) {
            certificateService.checkAndIssue(enrollment.getCourseId(), studentId);
        }
        return enrollment;
    }

    /** 수강 취소: 내 수강을 삭제하고 진도·수료증까지 함께 초기화한다(다시 수강하면 처음부터). */
    public void cancel(UUID enrollmentId, String studentId) {
        featureService.requireEnabled(Feature.ENROLLMENTS);
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                // RLS가 타 테넌트 행을 숨기고, 본인 수강이 아니면 노출하지 않는다(404)
                .filter(e -> e.getStudentId().equals(studentId))
                .orElseThrow(() -> new NotFoundException("수강 정보를 찾을 수 없습니다"));
        UUID courseId = enrollment.getCourseId();
        lessonProgressRepository.deleteByStudentIdAndCourseId(studentId, courseId);
        certificateService.removeForStudent(courseId, studentId);
        enrollmentRepository.delete(enrollment);
    }
}
