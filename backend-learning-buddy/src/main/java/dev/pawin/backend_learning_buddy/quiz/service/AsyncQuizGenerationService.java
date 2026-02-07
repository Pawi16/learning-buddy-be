package dev.pawin.backend_learning_buddy.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateQuizAiRequest;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateQuizAiResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.GenerateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizPreviewResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizTypeConfig;
import dev.pawin.backend_learning_buddy.quiz.dto.TopicQuizConfig;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizPreviewJobRepository;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AsyncQuizGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncQuizGenerationService.class);

    private final QuizPreviewJobRepository jobRepository;
    private final TopicRepository topicRepository;
    private final AiServiceClient aiServiceClient;
    private final Executor quizTaskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate transactionTemplate;

    @Async("quizTaskExecutor")
    public void generateQuizPreviewAsync(Long jobId, GenerateQuizRequest request) {
        logger.info("Starting async quiz generation for job ID: {}", jobId);

        try {
            // Update job status to PROCESSING
            updateJobStatus(jobId, JobStatus.PROCESSING);
            logger.info("Job {} status updated to PROCESSING", jobId);

            int totalTopics = request.getQuizTopics().size();
            AtomicInteger completedCounter = new AtomicInteger(0);

            // Create CompletableFuture for each topic (parallel execution)
            List<CompletableFuture<QuizPreviewResponse.TopicQuizPreview>> futures = request.getQuizTopics().stream()
                    .map(topicConfig -> CompletableFuture.supplyAsync(() -> {
                        return processTopic(topicConfig, jobId, completedCounter, totalTopics);
                    }, quizTaskExecutor))
                    .toList();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            List<QuizPreviewResponse.TopicQuizPreview> results = new ArrayList<>();
            for (CompletableFuture<QuizPreviewResponse.TopicQuizPreview> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    logger.error("Failed to get result from future", e);
                    throw new RuntimeException("Failed to process topic", e);
                }
            }

            // Serialize result to JSON
            String resultJson;
            try {
                resultJson = objectMapper.writeValueAsString(
                        QuizPreviewResponse.builder().topics(results).build()
                );
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize quiz results to JSON", e);
                markJobFailed(jobId, "Failed to serialize quiz results");
                return;
            }

            // Update job to COMPLETED
            updateJobCompleted(jobId, resultJson);
            logger.info("Job {} completed successfully with {} topics", jobId, results.size());

        } catch (Exception e) {
            logger.error("Quiz generation failed for job {}", jobId, e);
            markJobFailed(jobId, e.getMessage());
        }
    }

    private QuizPreviewResponse.TopicQuizPreview processTopic(
            TopicQuizConfig topicConfig,
            Long jobId,
            AtomicInteger completedCounter,
            int totalTopics
    ) {
        try {
            logger.info("Processing topic ID: {} for job: {}", topicConfig.getTopicId(), jobId);

            // Fetch topic details
            Topic topic = topicRepository.findById(topicConfig.getTopicId())
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicConfig.getTopicId()));

            // Build AI service request
            List<GenerateQuizAiRequest.QuizConfig> aiQuizConfig = topicConfig.getQuizConfig().stream()
                    .map(this::mapQuizConfig)
                    .toList();

            // Call AI service
            GenerateQuizAiResponse aiResponse = aiServiceClient.generateQuiz(
                    topic.getTitle(),
                    topic.getRawText() != null ? topic.getRawText() : topic.getDescription(),
                    aiQuizConfig
            );

            // Transform AI response to backend format
            List<QuizPreviewResponse.QuestionPreview> questions = aiResponse.getQuestions().stream()
                    .map(this::mapQuestion)
                    .toList();

            // Update progress
            int completed = completedCounter.incrementAndGet();
            int progress = (completed * 100) / totalTopics;
            updateJobProgressAtomic(jobId, progress, completed);
            logger.info("Topic {} completed. Progress: {}/{} ({}%)",
                    topicConfig.getTopicId(), completed, totalTopics, progress);

            return QuizPreviewResponse.TopicQuizPreview.builder()
                    .topicId(topic.getId())
                    .topicTitle(topic.getTitle())
                    .questions(questions)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to process topic {}", topicConfig.getTopicId(), e);
            throw new RuntimeException("Failed to process topic: " + topicConfig.getTopicId(), e);
        }
    }

    private GenerateQuizAiRequest.QuizConfig mapQuizConfig(QuizTypeConfig config) {
        List<GenerateQuizAiRequest.QuizTypeConfig> aiTypeConfigs = config.getTypes().stream()
                .map(typeConfig -> GenerateQuizAiRequest.QuizTypeConfig.builder()
                        .type(typeConfig.getType())
                        .number(typeConfig.getNumber())
                        .build())
                .toList();

        return GenerateQuizAiRequest.QuizConfig.builder()
                .difficulty(config.getDifficulty())
                .quizTypeConfig(aiTypeConfigs)
                .build();
    }

    private QuizPreviewResponse.QuestionPreview mapQuestion(GenerateQuizAiResponse.Question aiQuestion) {
        List<QuizPreviewResponse.ChoicePreview> choices = aiQuestion.getChoices().stream()
                .map(aiChoice -> QuizPreviewResponse.ChoicePreview.builder()
                        .choiceText(aiChoice.getChoiceText())
                        .isCorrect(aiChoice.getIsCorrect())
                        .build())
                .toList();

        return QuizPreviewResponse.QuestionPreview.builder()
                .questionText(aiQuestion.getQuestionText())
                .questionType(aiQuestion.getQuestionType())
                .difficultyLevel(aiQuestion.getDifficultyLevel())
                .explanation(aiQuestion.getExplanation())
                .choices(choices)
                .build();
    }

    protected void updateJobStatus(Long jobId, JobStatus status) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            QuizPreviewJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(status);
            jobRepository.save(job);
        });
    }

    protected synchronized void updateJobProgressAtomic(Long jobId, Integer progress, Integer completed) {
        // Use atomic UPDATE to prevent race conditions
        transactionTemplate.executeWithoutResult(txStatus -> {
            jobRepository.updateProgressAtomic(jobId, progress, completed);
        });
    }

    protected void updateJobCompleted(Long jobId, String resultJson) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            QuizPreviewJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedTopics(job.getTotalTopics());
            job.setResult(resultJson);
            jobRepository.save(job);
        });
    }

    protected void markJobFailed(Long jobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            QuizPreviewJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            jobRepository.save(job);
        });
    }
}
