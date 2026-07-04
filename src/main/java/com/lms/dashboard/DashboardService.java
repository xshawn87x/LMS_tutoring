package com.lms.dashboard;

import com.lms.certificate.CourseCompletionRepository;
import com.lms.course.Course;
import com.lms.course.CourseRepository;
import com.lms.course.CourseService;
import com.lms.dashboard.dto.CourseStatsResponse;
import com.lms.dashboard.dto.StudentProgressResponse;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.quiz.Quiz;
import com.lms.quiz.QuizRepository;
import com.lms.quiz.QuizSubmission;
import com.lms.quiz.QuizSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 강사/관리자용 과정 현황 집계. 모든 수치는 RLS로 현재 테넌트 범위에서만 계산된다.
 * (현재 모델엔 과정 소유자 개념이 없어 테넌트 내 전체 과정을 보여준다.)
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final CourseCompletionRepository completionRepository;
    private final CourseService courseService;

    public DashboardService(CourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            QuizRepository quizRepository,
                            QuizSubmissionRepository submissionRepository,
                            CourseCompletionRepository completionRepository,
                            CourseService courseService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
        this.completionRepository = completionRepository;
        this.courseService = courseService;
    }

    public List<CourseStatsResponse> courseStats() {
        List<CourseStatsResponse> out = new ArrayList<>();
        for (Course course : courseRepository.findAll()) {
            List<Enrollment> enrolls = enrollmentRepository.findByCourseId(course.getId());
            long enrollmentCount = enrolls.size();
            int avgProgress = enrolls.isEmpty() ? 0
                    : (int) Math.round(enrolls.stream().mapToInt(Enrollment::getProgress).average().orElse(0));
            long completed = enrolls.stream().filter(e -> e.getProgress() >= 100).count();

            List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            Integer avgQuizScore = avgQuizScore(quizzes);

            long certificateCount = completionRepository.countByCourseId(course.getId());

            out.add(new CourseStatsResponse(
                    course.getId(), course.getTitle(), course.getCategoryCode(), course.getLevel(),
                    enrollmentCount, avgProgress, completed,
                    quizzes.size(), avgQuizScore, certificateCount));
        }
        return out;
    }

    /** 한 과정의 수강생별 진도/성취 드릴다운. 과정이 현재 테넌트에 없으면 404. */
    public List<StudentProgressResponse> courseStudents(UUID courseId) {
        courseService.requireExists(courseId);

        List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        List<UUID> quizIds = quizzes.stream().map(Quiz::getId).toList();
        // 학생별 → (퀴즈별 최고 득점 비율)
        Map<String, Map<UUID, Double>> bestByStudent = quizIds.isEmpty() ? Map.of()
                : submissionRepository.findByQuizIdIn(quizIds).stream()
                        .filter(s -> s.getTotal() > 0)
                        .collect(Collectors.groupingBy(QuizSubmission::getStudentId,
                                Collectors.toMap(QuizSubmission::getQuizId,
                                        s -> (double) s.getScore() / s.getTotal(), Math::max)));

        List<StudentProgressResponse> out = new ArrayList<>();
        for (Enrollment e : enrollmentRepository.findByCourseId(courseId)) {
            Map<UUID, Double> best = bestByStudent.getOrDefault(e.getStudentId(), Map.of());
            int quizzesTaken = best.size();
            Integer avgQuizScore = best.isEmpty() ? null
                    : (int) Math.round(best.values().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100);
            boolean certified = completionRepository
                    .findByCourseIdAndStudentId(courseId, e.getStudentId()).isPresent();

            out.add(new StudentProgressResponse(
                    e.getStudentId(), e.getProgress(), e.getStatus().name(), e.getProgress() >= 100,
                    quizzesTaken, quizzes.size(), avgQuizScore, certified));
        }
        return out;
    }

    private Integer avgQuizScore(List<Quiz> quizzes) {
        if (quizzes.isEmpty()) {
            return null;
        }
        List<QuizSubmission> subs = submissionRepository.findByQuizIdIn(quizzes.stream().map(Quiz::getId).toList());
        OptionalDouble avg = subs.stream()
                .filter(s -> s.getTotal() > 0)
                .mapToDouble(s -> (double) s.getScore() / s.getTotal())
                .average();
        return avg.isPresent() ? (int) Math.round(avg.getAsDouble() * 100) : null;
    }
}
