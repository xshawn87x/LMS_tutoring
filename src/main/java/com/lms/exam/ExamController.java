package com.lms.exam;

import com.lms.exam.dto.ExamDtos.ExamRequest;
import com.lms.exam.dto.ExamDtos.ExamResponse;
import com.lms.exam.dto.ExamDtos.RecordScoresRequest;
import com.lms.exam.dto.ExamDtos.ScoreResponse;
import com.lms.exam.dto.ExamDtos.StudentScore;
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

/**
 * 시험/성적 API. 시험·성적 입력=INSTRUCTOR/ADMIN, 내 성적 조회=학생 본인.
 * (자녀 성적은 GuardianController의 리포트/성적 경로로 조회)
 */
@RestController
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    // --- 시험 관리 (INSTRUCTOR/ADMIN) ---

    @GetMapping("/api/exams")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<ExamResponse> list() {
        return service.list().stream().map(ExamResponse::from).toList();
    }

    @PostMapping("/api/exams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ExamResponse create(@RequestBody ExamRequest req) {
        return ExamResponse.from(service.create(req));
    }

    @PutMapping("/api/exams/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public ExamResponse update(@PathVariable UUID id, @RequestBody ExamRequest req) {
        return ExamResponse.from(service.update(id, req));
    }

    @DeleteMapping("/api/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    // --- 성적 입력/조회 (INSTRUCTOR/ADMIN) ---

    @GetMapping("/api/exams/{id}/scores")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<ScoreResponse> scores(@PathVariable UUID id) {
        return service.scoresForExam(id).stream().map(ScoreResponse::from).toList();
    }

    @PostMapping("/api/exams/{id}/scores")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<ScoreResponse> recordScores(@PathVariable UUID id, @RequestBody RecordScoresRequest req) {
        return service.recordScores(id, req.entries()).stream().map(ScoreResponse::from).toList();
    }

    // --- 내 성적 (학생 본인) ---

    @GetMapping("/api/me/scores")
    public List<StudentScore> myScores(@AuthenticationPrincipal Jwt jwt) {
        return service.studentScores(jwt.getSubject());
    }
}
