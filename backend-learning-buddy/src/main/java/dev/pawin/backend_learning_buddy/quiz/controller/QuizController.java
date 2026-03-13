package dev.pawin.backend_learning_buddy.quiz.controller;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizMetadataResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizResultResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.SubmitQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizContentRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizContentResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizMetadataRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizMetadataResponse;
import dev.pawin.backend_learning_buddy.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/{id}/attempt")
    public ResponseEntity<QuizExamDetailResponse> getQuizForAttempt(
            @PathVariable Long id,
            Authentication authentication
    ) {
        QuizExamDetailResponse response = quizService.getQuizForAttempt(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/attempt")
    public ResponseEntity<QuizResultResponse> submitQuizAttempt(
            @PathVariable Long id,
            @Valid @RequestBody SubmitQuizRequest request,
            Authentication authentication
    ) {
        QuizResultResponse response = quizService.submitQuizAttempt(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> getQuizDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        QuizDetailResponse response = quizService.getQuizDetail(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<QuizMetadataResponse> updateQuizMetadata(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuizMetadataRequest request,
            Authentication authentication
    ) {
        UpdateQuizMetadataResponse response = quizService.updateQuizMetadata(id, authentication.getName(), request);
        return ResponseEntity.ok(response.getQuizMetadata());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateQuizContentResponse> updateQuizContent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuizContentRequest request,
            Authentication authentication
    ) {
        UpdateQuizContentResponse response = quizService.updateQuizContent(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
