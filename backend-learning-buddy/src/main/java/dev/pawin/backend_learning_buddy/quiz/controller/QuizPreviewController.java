package dev.pawin.backend_learning_buddy.quiz.controller;

import dev.pawin.backend_learning_buddy.quiz.dto.GenerateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStartResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStatusResponse;
import dev.pawin.backend_learning_buddy.quiz.service.QuizPreviewService;
import dev.pawin.backend_learning_buddy.auth.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quizzes/preview")
@RequiredArgsConstructor
public class QuizPreviewController {

    private static final Logger logger = LoggerFactory.getLogger(QuizPreviewController.class);

    private final QuizPreviewService quizPreviewService;

    @PostMapping("/jobs")
    public ResponseEntity<JobStartResponse> generateQuizPreview(
            @Valid @RequestBody GenerateQuizRequest request,
            @AuthenticationPrincipal User user
    ) {
        logger.info("Received quiz generation request from user: {}", user.getUsername());

        JobStartResponse response = quizPreviewService.startQuizGenerationJob(request, user);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User user
    ) {
        logger.info("Fetching job status for job ID: {} by user: {}", jobId, user.getUsername());

        JobStatusResponse response = quizPreviewService.getJobStatus(jobId, user);

        return ResponseEntity.ok(response);
    }
}
