package com.lms.dashboard;

import com.lms.dashboard.dto.CourseStatsResponse;
import com.lms.dashboard.dto.StudentProgressResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 강사/관리자용 과정 현황 대시보드 API. */
@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/instructor/courses")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<CourseStatsResponse> courseStats() {
        return service.courseStats();
    }

    @GetMapping("/api/instructor/courses/{courseId}/students")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public List<StudentProgressResponse> courseStudents(@PathVariable UUID courseId) {
        return service.courseStudents(courseId);
    }
}
