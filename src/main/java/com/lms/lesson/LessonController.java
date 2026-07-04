package com.lms.lesson;

import com.lms.lesson.dto.LessonRequest;
import com.lms.lesson.dto.LessonResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons")
public class LessonController {

    private final LessonService service;

    public LessonController(LessonService service) {
        this.service = service;
    }

    @GetMapping
    public List<LessonResponse> list(@PathVariable UUID courseId) {
        return service.listByCourse(courseId).stream().map(LessonResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public LessonResponse add(@PathVariable UUID courseId, @Valid @RequestBody LessonRequest request) {
        return LessonResponse.from(service.add(courseId, request));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public LessonResponse update(@PathVariable UUID courseId, @PathVariable UUID lessonId,
                                 @Valid @RequestBody LessonRequest request) {
        return LessonResponse.from(service.update(courseId, lessonId, request));
    }

    @DeleteMapping("/{lessonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public void delete(@PathVariable UUID courseId, @PathVariable UUID lessonId) {
        service.delete(courseId, lessonId);
    }
}
