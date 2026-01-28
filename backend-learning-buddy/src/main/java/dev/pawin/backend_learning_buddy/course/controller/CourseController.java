package dev.pawin.backend_learning_buddy.course.controller;

import dev.pawin.backend_learning_buddy.course.dto.*;
import dev.pawin.backend_learning_buddy.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping
    public  ResponseEntity<List<CourseSummaryResponse>> getPublicCourses(
            @RequestParam(required = false) String search
    ){
        List<CourseSummaryResponse> response = courseService.getPublicCourses(search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @PathVariable Long id,
            Authentication authentication
    ){
        CourseDetailResponse response = courseService.getCourseDetail(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateCourseResponse> updateCourseMetadata (
            @PathVariable Long id,
            @RequestBody UpdateCourseRequest request,
            Authentication authentication
    ){
        UpdateCourseResponse response = courseService.updateCourseMetadata(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

}