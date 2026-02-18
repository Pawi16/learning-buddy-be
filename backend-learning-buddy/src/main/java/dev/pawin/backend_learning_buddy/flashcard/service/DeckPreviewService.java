package dev.pawin.backend_learning_buddy.flashcard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.common.exception.DeckJobNotFoundException;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.EnrollmentRepository;
import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckJobStatusResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckPreviewResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.GenerateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.entity.DeckPreviewJob;
import dev.pawin.backend_learning_buddy.flashcard.event.DeckJobStartedEvent;
import dev.pawin.backend_learning_buddy.flashcard.repository.DeckPreviewJobRepository;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStartResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeckPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(DeckPreviewService.class);

    private final DeckPreviewJobRepository jobRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AsyncDeckGenerationService asyncDeckGenerationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public JobStartResponse startDeckGenerationJob(GenerateDeckRequest request, User user) {
        logger.info("Starting deck generation job for user: {}", user.getUsername());

        // Pre-flight validation: verify user has access to all topics
        validateTopicAccess(request, user);

        // Create job entity
        UUID jobId = UUID.randomUUID();
        DeckPreviewJob job = DeckPreviewJob.builder()
                .jobId(jobId)
                .user(user)
                .status(JobStatus.QUEUED)
                .progressPercent(0)
                .totalTopics(request.getDeckTopics().size())
                .completedTopics(0)
                .build();

        job = jobRepository.save(job);
        logger.info("Created deck preview job with ID: {}", jobId);

        // Publish event to trigger async processing after transaction commits
        eventPublisher.publishEvent(new DeckJobStartedEvent(job.getJobId(), request));

        return JobStartResponse.builder()
                .jobId(jobId.toString())
                .status(JobStatus.QUEUED.name())
                .message("Deck generation job started successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public DeckJobStatusResponse getJobStatus(UUID jobId, User user) {
        logger.info("Fetching job status for job ID: {}", jobId);

        DeckPreviewJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new DeckJobNotFoundException("Deck job not found: " + jobId));

        // Verify ownership
        if (!job.getUser().getId().equals(user.getId())) {
            throw new DeckJobNotFoundException("Deck job not found: " + jobId);
        }

        // Parse result if completed
        DeckPreviewResponse result = null;
        if (job.getStatus() == JobStatus.COMPLETED && job.getResult() != null) {
            try {
                result = objectMapper.readValue(job.getResult(), DeckPreviewResponse.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse deck result for job {}", jobId, e);
            }
        }

        return DeckJobStatusResponse.builder()
                .jobId(job.getJobId().toString())
                .status(job.getStatus().name())
                .progressPercent(job.getProgressPercent())
                .errorMessage(job.getErrorMessage())
                .result(result)
                .build();
    }

    private void validateTopicAccess(GenerateDeckRequest request, User user) {
        for (var topicConfig : request.getDeckTopics()) {
            // Fetch topic to get course
            var topic = courseRepository.findTopicById(topicConfig.getTopicId())
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicConfig.getTopicId()));

            Course course = topic.getCourse();

            // Check if user is creator or enrolled
            boolean isCreator = course.getCreator() != null && course.getCreator().getId().equals(user.getId());
            boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId());

            if (!isCreator && !isEnrolled) {
                throw new IllegalArgumentException("You don't have access to topic: " + topicConfig.getTopicId());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeckJobStarted(DeckJobStartedEvent event) {
        logger.info("Transaction committed, starting async processing for job: {}", event.getJobId());
        asyncDeckGenerationService.generateDeckPreviewAsync(event.getJobId(), event.getRequest());
    }
}
