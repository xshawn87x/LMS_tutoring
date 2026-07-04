package com.lms.certificate;

import com.lms.certificate.dto.CertificateResponse;
import com.lms.course.Course;
import com.lms.course.CourseRepository;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.feature.Feature;
import com.lms.feature.FeatureService;
import com.lms.quiz.Quiz;
import com.lms.quiz.QuizRepository;
import com.lms.quiz.QuizSubmission;
import com.lms.quiz.QuizSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 수료 판정 + 수료증 발급.
 * 수료 조건: 진도 100% (status COMPLETED) + 과정의 <b>모든 퀴즈를 60% 이상으로 통과</b>.
 * 퀴즈가 없는 과정은 진도 100%만으로 수료. CERTIFICATES 기능이 켜진 기관에서만 동작.
 */
@Service
@Transactional
public class CertificateService {

    private static final double PASS_RATIO = 0.6;

    private final CourseCompletionRepository completionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final FeatureService featureService;

    public CertificateService(CourseCompletionRepository completionRepository,
                              EnrollmentRepository enrollmentRepository,
                              QuizRepository quizRepository,
                              QuizSubmissionRepository submissionRepository,
                              CourseRepository courseRepository,
                              FeatureService featureService) {
        this.completionRepository = completionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.featureService = featureService;
    }

    /** 수료 조건 충족 시 수료증을 발급(이미 있으면 그대로 반환). 미충족/기능 OFF면 null. */
    public CourseCompletion checkAndIssue(UUID courseId, String studentId) {
        if (!featureService.isEnabled(Feature.CERTIFICATES)) {
            return null;
        }
        Optional<CourseCompletion> existing = completionRepository.findByCourseIdAndStudentId(courseId, studentId);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!isComplete(courseId, studentId)) {
            return null;
        }
        return completionRepository.save(new CourseCompletion(courseId, studentId, generateCertificateNo()));
    }

    /** 수료 조건 충족 여부 (발급과 무관하게 계산). */
    @Transactional(readOnly = true)
    public boolean isComplete(UUID courseId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId).orElse(null);
        if (enrollment == null || enrollment.getProgress() < 100) {
            return false;
        }
        List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        if (quizzes.isEmpty()) {
            return true;
        }
        // 학생의 퀴즈별 최고 득점 비율
        Map<UUID, Double> bestRatio = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
                .filter(s -> s.getTotal() > 0)
                .collect(Collectors.toMap(
                        QuizSubmission::getQuizId,
                        s -> (double) s.getScore() / s.getTotal(),
                        Math::max));
        return quizzes.stream().allMatch(q -> bestRatio.getOrDefault(q.getId(), 0.0) >= PASS_RATIO);
    }

    /** 수강 취소 시 해당 과정의 내 수료증(있다면) 제거 — 상태 완전 초기화. */
    public void removeForStudent(UUID courseId, String studentId) {
        completionRepository.findByCourseIdAndStudentId(courseId, studentId)
                .ifPresent(completionRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> myCertificates(String studentId) {
        featureService.requireEnabled(Feature.CERTIFICATES);
        List<CourseCompletion> list = completionRepository.findByStudentIdOrderByIssuedAtDesc(studentId);
        Map<UUID, String> titles = titlesFor(list.stream().map(CourseCompletion::getCourseId).toList());
        return list.stream()
                .map(c -> CertificateResponse.from(c, titles.get(c.getCourseId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getForCourse(UUID courseId, String studentId) {
        featureService.requireEnabled(Feature.CERTIFICATES);
        return completionRepository.findByCourseIdAndStudentId(courseId, studentId)
                .map(c -> CertificateResponse.from(c, courseRepository.findById(courseId)
                        .map(Course::getTitle).orElse(null)))
                .orElse(null);
    }

    private Map<UUID, String> titlesFor(List<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (a, b) -> a));
    }

    private String generateCertificateNo() {
        return "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
