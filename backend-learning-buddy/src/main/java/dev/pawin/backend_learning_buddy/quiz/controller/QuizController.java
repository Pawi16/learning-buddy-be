package dev.pawin.backend_learning_buddy.quiz.controller;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
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
}
