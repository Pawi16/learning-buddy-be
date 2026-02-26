package dev.pawin.backend_learning_buddy.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.common.exception.CourseJobNotFoundException;
import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.course.dto.CourseJobStartResponse;
import dev.pawin.backend_learning_buddy.course.dto.CourseJobStatusResponse;
import dev.pawin.backend_learning_buddy.course.dto.CoursePreviewResponse;
import dev.pawin.backend_learning_buddy.course.dto.GenerateCoursePreviewRequest;
import dev.pawin.backend_learning_buddy.course.entity.CoursePreviewJob;
import dev.pawin.backend_learning_buddy.course.event.CourseJobStartedEvent;
import dev.pawin.backend_learning_buddy.course.repository.CoursePreviewJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoursePreviewService {

    private static final Logger logger = LoggerFactory.getLogger(CoursePreviewService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final CoursePreviewJobRepository jobRepository;
    private final AsyncCourseGenerationService asyncCourseGenerationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CourseJobStartResponse startCoursePreviewJob(
            GenerateCoursePreviewRequest request,
            User user
    ) {
        logger.info("Starting course preview job for user: {}", user.getUsername());

        // Validate request
        validateRequest(request);

        UUID jobId = UUID.randomUUID();
        CoursePreviewJob job = CoursePreviewJob.builder()
                .jobId(jobId)
                .user(user)
                .status(JobStatus.QUEUED)
                .build();

        job = jobRepository.save(job);
        logger.info("Created course preview job with ID: {}", jobId);

        // Publish event for async processing after transaction commits
        MultipartFile file = request.getFile();
        try {
            eventPublisher.publishEvent(new CourseJobStartedEvent(
                    jobId,
                    request.getTitle(),
                    request.getDescription(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            ));
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Failed to read file content", e);
        }

        return CourseJobStartResponse.builder()
                .jobId(jobId.toString())
                .status(JobStatus.QUEUED.name())
                .message("Course preview job started successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public CourseJobStatusResponse getJobStatus(UUID jobId, User user) {
        logger.info("Fetching course preview job status for job ID: {}", jobId);

        CoursePreviewJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new CourseJobNotFoundException("Course job not found: " + jobId));

        // Verify ownership
        if (!job.getUser().getId().equals(user.getId())) {
            throw new CourseJobNotFoundException("Course job not found: " + jobId);
        }

        // Parse result if completed
        CoursePreviewResponse result = null;
        if (job.getStatus() == JobStatus.COMPLETED && job.getResult() != null) {
            try {
                result = objectMapper.readValue(job.getResult(), CoursePreviewResponse.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse course preview result for job {}", jobId, e);
            }
        }

        return CourseJobStatusResponse.builder()
                .jobId(job.getJobId().toString())
                .status(job.getStatus().name())
                .errorMessage(job.getErrorMessage())
                .result(result)
                .build();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCourseJobStarted(CourseJobStartedEvent event) {
        logger.info("Transaction committed, starting async processing for course preview job: {}", event.getJobId());
        asyncCourseGenerationService.generateCoursePreviewAsync(
                event.getJobId(),
                event.getTitle(),
                event.getDescription(),
                event.getFilename(),
                event.getContentType(),
                event.getFileContent()
        );
    }

    private void validateRequest(GenerateCoursePreviewRequest request) {
        // Validate title
        if (request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Course title cannot be empty");
        }

        // Validate file
        MultipartFile file = request.getFile();
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File is too large. Maximum allowed size is 10MB.");
        }

        // Check content type
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed. Received: " + file.getContentType());
        }
    }
}
