package dev.pawin.backend_learning_buddy.course.controller;

import dev.pawin.backend_learning_buddy.course.dto.*;
import dev.pawin.backend_learning_buddy.course.service.CourseService;
import dev.pawin.backend_learning_buddy.course.service.EnrollmentService;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckSummaryResponse;
import dev.pawin.backend_learning_buddy.flashcard.service.DeckService;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.service.QuizService;
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
    private final EnrollmentService enrollmentService;
    private final QuizService quizService;
    private final DeckService deckService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteCourseResponse> deleteCourse (
            @PathVariable Long id,
            Authentication authentication
    ){
        DeleteCourseResponse response = courseService.deleteCourse(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<CourseContentResponse> getCourseContent(
            @PathVariable Long id,
            Authentication authentication
    ){
        CourseContentResponse response = courseService.getCourseContent(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/content")
    public ResponseEntity<UpdateCourseContentResponse> updateCourseContent(
            @PathVariable Long id,
            @RequestBody UpdateCourseContentRequest request,
            Authentication authentication
    ){
        UpdateCourseContentResponse response = courseService.updateCourseContent(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<CourseSummaryResponse>> getMyCourses (
            Authentication authentication
    ){
        List<CourseSummaryResponse> myCourses = courseService.getMyCourses(authentication.getName());

        return ResponseEntity.ok(myCourses);
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<EnrollCourseResponse> enrollInCourse(
            @PathVariable Long id,
            Authentication authentication
    ) {
        EnrollCourseResponse response = enrollmentService.enrollUser(id, authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/enrolled")
    public ResponseEntity<List<CourseSummaryResponse>> getMyEnrolledCourses(Authentication authentication) {
        List<CourseSummaryResponse> courses = courseService.getEnrolledCourses(authentication.getName());
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/{id}/quizzes")
    public ResponseEntity<CreateQuizResponse> createQuiz(
            @PathVariable Long id,
            @Valid @RequestBody CreateQuizRequest request,
            Authentication authentication
    ) {
        CreateQuizResponse response = quizService.createQuiz(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/decks")
    public ResponseEntity<CreateDeckResponse> createDeck(
            @PathVariable Long id,
            @Valid @RequestBody CreateDeckRequest request,
            Authentication authentication
    ) {
        CreateDeckResponse response = deckService.createDeck(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<List<QuizSummaryResponse>> getCourseQuizzes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        List<QuizSummaryResponse> quizzes = quizService.getQuizzesByCourseId(
                id, authentication.getName());
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/{id}/decks")
    public ResponseEntity<List<DeckSummaryResponse>> getCourseDecks(
            @PathVariable Long id,
            Authentication authentication
    ) {
        List<DeckSummaryResponse> decks = deckService.getDecksByCourseId(
                id, authentication.getName());
        return ResponseEntity.ok(decks);
    }
}