package dev.pawin.backend_learning_buddy.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.course.dto.CoursePreviewResponse;
import dev.pawin.backend_learning_buddy.course.dto.TopicPreviewDto;
import dev.pawin.backend_learning_buddy.course.entity.CoursePreviewJob;
import dev.pawin.backend_learning_buddy.course.repository.CoursePreviewJobRepository;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class AsyncCourseGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCourseGenerationService.class);

    private final CoursePreviewJobRepository jobRepository;
    private final AiServiceClient aiServiceClient;
    private final Executor courseTaskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate transactionTemplate;

    @Async("courseTaskExecutor")
    public void generateCoursePreviewAsync(UUID jobId, String title, String description,
                                           String filename, String contentType, byte[] fileContent) {
        logger.info("Starting async course preview generation for job ID: {}", jobId);

        try {
            // Update job status to PROCESSING
            updateJobStatus(jobId, JobStatus.PROCESSING);
            logger.info("Job {} status updated to PROCESSING", jobId);

            // Call AI service (reuse existing aiServiceClient.generateCoursePreview)
            List<TopicPreviewDto> topics = aiServiceClient.generateCoursePreview(
                    filename, contentType, fileContent
            );

            // Build result
            CoursePreviewResponse previewResponse = CoursePreviewResponse.builder()
                    .title(title)
                    .description(description)
                    .topics(topics)
                    .build();

            // Serialize result to JSON
            String resultJson = objectMapper.writeValueAsString(previewResponse);

            // Update job to COMPLETED
            updateJobCompleted(jobId, resultJson);
            logger.info("Job {} completed successfully with {} topics", jobId, topics.size());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize course preview result for job {}", jobId, e);
            markJobFailed(jobId, "Failed to serialize course preview result");
        } catch (Exception e) {
            logger.error("Course preview generation failed for job {}", jobId, e);
            markJobFailed(jobId, e.getMessage());
        }
    }

    protected void updateJobStatus(UUID jobId, JobStatus status) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            CoursePreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(status);
            jobRepository.save(job);
        });
    }

    protected void updateJobCompleted(UUID jobId, String resultJson) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            CoursePreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.COMPLETED);
            job.setResult(resultJson);
            jobRepository.save(job);
        });
    }

    protected void markJobFailed(UUID jobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            CoursePreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            jobRepository.save(job);
        });
    }
}
