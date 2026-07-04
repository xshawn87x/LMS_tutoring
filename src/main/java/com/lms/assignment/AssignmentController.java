package com.lms.assignment;

import com.lms.assignment.dto.AssignmentDtos.AssignmentRequest;
import com.lms.assignment.dto.AssignmentDtos.AssignmentResponse;
import com.lms.assignment.dto.AssignmentDtos.GradeRequest;
import com.lms.assignment.dto.AssignmentDtos.SubmissionResponse;
import com.lms.assignment.dto.AssignmentDtos.SubmitRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 과제 API. 관리·채점=INSTRUCTOR/ADMIN, 제출=STUDENT 본인. */
@RestController
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) {
        this.service = service;
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    public List<AssignmentResponse> list(@PathVariable UUID courseId) {
        return service.list(courseId).stream().map(AssignmentResponse::from).toList();
    }

    @PostMapping("/api/courses/{courseId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public AssignmentResponse create(@PathVariable UUID courseId, @Valid @RequestBody AssignmentRequest request) {
        return AssignmentResponse.from(service.create(courseId, request));
    }

    @GetMapping("/api/assignments/{id}")
    public AssignmentResponse get(@PathVariable UUID id) {
        return AssignmentResponse.from(service.get(id));
    }

    @PutMapping("/api/assignments/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public AssignmentResponse update(@PathVariable UUID id, @Valid @RequestBody AssignmentRequest request) {
        return AssignmentResponse.from(service.update(id, request));
    }

    @DeleteMapping("/api/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    // --- 제출/채점 ---

    @PostMapping("/api/assignments/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public SubmissionResponse submit(@PathVariable UUID id, @Valid @RequestBody SubmitRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return SubmissionResponse.from(service.submit(id, jwt.getSubject(), request.textAnswer(), request.fileUrl()));
    }

    /** 강사/관리자: 전체 제출 목록. */
    @GetMapping("/api/assignments/{id}/submissions")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<SubmissionResponse> submissions(@PathVariable UUID id) {
        return service.submissions(id).stream().map(SubmissionResponse::from).toList();
    }

    /** 학생: 내 제출. */
    @GetMapping("/api/assignments/{id}/my-submission")
    public SubmissionResponse mySubmission(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return SubmissionResponse.from(service.mySubmission(id, jwt.getSubject()));
    }

    @PostMapping("/api/submissions/{submissionId}/grade")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public SubmissionResponse grade(@PathVariable UUID submissionId, @Valid @RequestBody GradeRequest request) {
        return SubmissionResponse.from(service.grade(submissionId, request.score(), request.feedback()));
    }
}
