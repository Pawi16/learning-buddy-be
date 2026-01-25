package dev.pawin.backend_learning_buddy.course.controller;

import dev.pawin.backend_learning_buddy.course.dto.CoursePreviewResponse;
import dev.pawin.backend_learning_buddy.course.dto.CreateCourseRequest;
import dev.pawin.backend_learning_buddy.course.dto.CreateCourseResponse;
import dev.pawin.backend_learning_buddy.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    public ResponseEntity<CoursePreviewResponse> previewCourse(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file
    ) {
        CoursePreviewResponse response = courseService.previewCourse(title, description, file);
        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<CreateCourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication
            ) {
        String username = authentication.getName();

        CreateCourseResponse response = courseService.createCourse(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}