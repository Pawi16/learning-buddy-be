package dev.pawin.backend_learning_buddy.flashcard.controller;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckJobStatusResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.GenerateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.service.DeckPreviewService;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStartResponse;
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
@RequestMapping("/api/v1/decks/preview")
@RequiredArgsConstructor
public class DeckPreviewController {

    private static final Logger logger = LoggerFactory.getLogger(DeckPreviewController.class);

    private final DeckPreviewService deckPreviewService;

    @PostMapping("/jobs")
    public ResponseEntity<JobStartResponse> generateDeckPreview(
            @Valid @RequestBody GenerateDeckRequest request,
            @AuthenticationPrincipal User user
    ) {
        logger.info("Received deck generation request from user: {}", user.getUsername());

        JobStartResponse response = deckPreviewService.startDeckGenerationJob(request, user);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<DeckJobStatusResponse> getJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User user
    ) {
        logger.info("Fetching job status for job ID: {} by user: {}", jobId, user.getUsername());

        DeckJobStatusResponse response = deckPreviewService.getJobStatus(jobId, user);

        return ResponseEntity.ok(response);
    }
}
