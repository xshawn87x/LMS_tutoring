package com.lms.assignment;

import com.lms.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.lms.course.CourseService;
import com.lms.error.NotFoundException;
import com.lms.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 과제 CRUD + 제출/채점. RLS로 테넌트 격리.
 * 과제 관리·채점=INSTRUCTOR/ADMIN(컨트롤러), 제출=STUDENT 본인.
 */
@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;

    public AssignmentService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                             CourseService courseService, NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Assignment> list(UUID courseId) {
        courseService.requireExists(courseId);
        return assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    @Transactional(readOnly = true)
    public Assignment get(UUID id) {
        return requireAssignment(id);
    }

    public Assignment create(UUID courseId, AssignmentRequest req) {
        courseService.requireExists(courseId);
        return assignmentRepository.save(
                new Assignment(courseId, req.title(), req.description(), req.dueAt(), req.maxScore()));
    }

    public Assignment update(UUID id, AssignmentRequest req) {
        Assignment a = requireAssignment(id);
        a.update(req.title(), req.description(), req.dueAt(), req.maxScore());
        return a;
    }

    public void delete(UUID id) {
        assignmentRepository.delete(requireAssignment(id));   // 제출은 FK CASCADE
    }

    // --- 제출/채점 ---

    /** 학생 제출(재제출은 갱신 + 채점 초기화). */
    public AssignmentSubmission submit(UUID assignmentId, String student, String textAnswer, String fileUrl) {
        requireAssignment(assignmentId);
        return submissionRepository.findByAssignmentIdAndStudent(assignmentId, student)
                .map(existing -> { existing.resubmit(textAnswer, fileUrl); return existing; })
                .orElseGet(() -> submissionRepository.save(
                        new AssignmentSubmission(assignmentId, student, textAnswer, fileUrl)));
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmission> submissions(UUID assignmentId) {
        requireAssignment(assignmentId);
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmission mySubmission(UUID assignmentId, String student) {
        return submissionRepository.findByAssignmentIdAndStudent(assignmentId, student)
                .orElseThrow(() -> new NotFoundException("제출 내역이 없습니다"));
    }

    public AssignmentSubmission grade(UUID submissionId, int score, String feedback) {
        AssignmentSubmission s = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("제출을 찾을 수 없습니다: " + submissionId));
        s.grade(score, feedback);
        // 채점 완료 → 학생에게 인앱 알림
        notificationService.notify(s.getStudent(), "과제가 채점되었습니다",
                String.format("점수 %d점%s", score, feedback == null || feedback.isBlank() ? "" : " · " + feedback));
        return s;
    }

    private Assignment requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("과제를 찾을 수 없습니다: " + id));
    }
}
