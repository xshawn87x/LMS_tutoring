package com.lms.course;

import com.lms.course.dto.CourseRequest;
import com.lms.course.dto.CourseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @GetMapping
    public List<CourseResponse> list(Authentication auth) {
        // 강사/관리자는 비공개 포함 전체, 그 외(학생 등)는 공개 과정만.
        boolean canSeeHidden = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR") || a.getAuthority().equals("ROLE_ADMIN"));
        List<Course> courses = canSeeHidden ? service.findAll() : service.findVisible();
        return courses.stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> get(@PathVariable UUID id) {
        // RLS가 다른 테넌트의 행을 숨기므로, 교차 테넌트 조회는 자연히 404가 된다.
        return service.findById(id)
                .map(CourseResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseRequest request) {
        return CourseResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    /** 강의 노출 토글 (공개/비공개). */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public CourseResponse publish(@PathVariable UUID id, @RequestParam boolean published) {
        return CourseResponse.from(service.setPublished(id, published));
    }

    /** 수강료 설정. */
    @PutMapping("/{id}/tuition")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public CourseResponse tuition(@PathVariable UUID id, @RequestParam int fee) {
        return CourseResponse.from(service.setTuition(id, fee));
    }
}
