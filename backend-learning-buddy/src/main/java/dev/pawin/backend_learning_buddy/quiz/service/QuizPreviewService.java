package dev.pawin.backend_learning_buddy.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.common.exception.QuizJobNotFoundException;
import dev.pawin.backend_learning_buddy.quiz.dto.GenerateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStartResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.JobStatusResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizPreviewResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizPreviewJobRepository;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.EnrollmentRepository;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.quiz.event.QuizJobStartedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuizPreviewService {

    private static final Logger logger = LoggerFactory.getLogger(QuizPreviewService.class);

    private final QuizPreviewJobRepository jobRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AsyncQuizGenerationService asyncQuizGenerationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public JobStartResponse startQuizGenerationJob(GenerateQuizRequest request, User user) {
        logger.info("Starting quiz generation job for user: {}", user.getUsername());

        // Validate course ownership and get course entity
        Course course = validateCourseOwnership(request.getCourseId(), user);

        // Pre-flight validation: verify user has access to all topics
        validateTopicAccess(request, user);

        // Create job entity with course association
        UUID jobId = UUID.randomUUID();
        QuizPreviewJob job = QuizPreviewJob.builder()
                .jobId(jobId)
                .course(course)
                .user(user)
                .status(JobStatus.QUEUED)
                .progressPercent(0)
                .totalTopics(request.getQuizTopics().size())
                .completedTopics(0)
                .build();

        job = jobRepository.save(job);
        logger.info("Created quiz preview job with ID: {}", jobId);

        // Publish event to trigger async processing after transaction commits
        eventPublisher.publishEvent(new QuizJobStartedEvent(job.getJobId(), request));

        return JobStartResponse.builder()
                .jobId(jobId.toString())
                .status(JobStatus.QUEUED.name())
                .message("Quiz generation job started successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(UUID jobId, User user) {
        logger.info("Fetching job status for job ID: {}", jobId);

        QuizPreviewJob job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new QuizJobNotFoundException("Quiz job not found: " + jobId));

        // Verify ownership - user must be the job creator
        if (!job.getUser().getId().equals(user.getId())) {
            throw new QuizJobNotFoundException("Quiz job not found: " + jobId);
        }

        // Additional check: verify user still owns the course
        if (job.getCourse().getCreator() == null ||
                !job.getCourse().getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access to this job has been revoked.");
        }

        // Parse result if completed
        QuizPreviewResponse result = null;
        if (job.getStatus() == JobStatus.COMPLETED && job.getResult() != null) {
            try {
                result = objectMapper.readValue(job.getResult(), QuizPreviewResponse.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse quiz result for job {}", jobId, e);
            }
        }

        return JobStatusResponse.builder()
                .jobId(job.getJobId().toString())
                .status(job.getStatus().name())
                .progressPercent(job.getProgressPercent())
                .errorMessage(job.getErrorMessage())
                .result(result)
                .build();
    }

    private Course validateCourseOwnership(Long courseId, User user) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // Check if user is the course creator
        if (course.getCreator() == null || !course.getCreator().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You do not have permission to generate quiz previews for this course.");
        }

        return course;
    }

    private void validateTopicAccess(GenerateQuizRequest request, User user) {
        for (var topicConfig : request.getQuizTopics()) {
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
    public void onQuizJobStarted(QuizJobStartedEvent event) {
        logger.info("Transaction committed, starting async processing for job: {}", event.getJobId());
        asyncQuizGenerationService.generateQuizPreviewAsync(event.getJobId(), event.getRequest());
    }
}
