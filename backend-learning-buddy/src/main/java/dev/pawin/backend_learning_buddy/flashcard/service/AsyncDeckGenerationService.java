package dev.pawin.backend_learning_buddy.flashcard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckPreviewResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckTopicConfig;
import dev.pawin.backend_learning_buddy.flashcard.dto.GenerateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.entity.DeckPreviewJob;
import dev.pawin.backend_learning_buddy.flashcard.repository.DeckPreviewJobRepository;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateFlashcardAiResponse;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AsyncDeckGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncDeckGenerationService.class);

    private final DeckPreviewJobRepository jobRepository;
    private final TopicRepository topicRepository;
    private final AiServiceClient aiServiceClient;
    private final Executor quizTaskExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate transactionTemplate;

    @Async("quizTaskExecutor")
    public void generateDeckPreviewAsync(UUID jobId, GenerateDeckRequest request) {
        logger.info("Starting async deck generation for job ID: {}", jobId);

        try {
            // Update job status to PROCESSING
            updateJobStatus(jobId, JobStatus.PROCESSING);
            logger.info("Job {} status updated to PROCESSING", jobId);

            int totalTopics = request.getDeckTopics().size();
            AtomicInteger completedCounter = new AtomicInteger(0);

            // Create CompletableFuture for each topic (parallel execution)
            List<CompletableFuture<List<DeckPreviewResponse.GeneratedCard>>> futures = request.getDeckTopics().stream()
                    .map(topicConfig -> CompletableFuture.supplyAsync(() -> {
                        return processTopic(topicConfig, jobId, completedCounter, totalTopics);
                    }, quizTaskExecutor))
                    .toList();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect all cards into a flat list
            List<DeckPreviewResponse.GeneratedCard> allCards = new ArrayList<>();
            for (CompletableFuture<List<DeckPreviewResponse.GeneratedCard>> future : futures) {
                try {
                    allCards.addAll(future.get());
                } catch (Exception e) {
                    logger.error("Failed to get result from future", e);
                    throw new RuntimeException("Failed to process topic", e);
                }
            }

            // Serialize result to JSON
            String resultJson;
            try {
                resultJson = objectMapper.writeValueAsString(
                        DeckPreviewResponse.builder()
                                .generatedCards(allCards)
                                .build()
                );
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize deck results to JSON", e);
                markJobFailed(jobId, "Failed to serialize deck results");
                return;
            }

            // Update job to COMPLETED
            updateJobCompleted(jobId, resultJson);
            logger.info("Job {} completed successfully with {} cards", jobId, allCards.size());

        } catch (Exception e) {
            logger.error("Deck generation failed for job {}", jobId, e);
            markJobFailed(jobId, e.getMessage());
        }
    }

    private List<DeckPreviewResponse.GeneratedCard> processTopic(
            DeckTopicConfig topicConfig,
            UUID jobId,
            AtomicInteger completedCounter,
            int totalTopics
    ) {
        try {
            logger.info("Processing topic ID: {} for job: {}", topicConfig.getTopicId(), jobId);

            // Fetch topic details
            Topic topic = topicRepository.findById(topicConfig.getTopicId())
                    .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicConfig.getTopicId()));

            // Call AI service
            GenerateFlashcardAiResponse aiResponse = aiServiceClient.generateFlashcard(
                    topic.getTitle(),
                    topic.getRawText() != null ? topic.getRawText() : topic.getDescription(),
                    topicConfig.getAmount()
            );

            // Transform AI response to backend format with topic_id
            List<DeckPreviewResponse.GeneratedCard> cards = aiResponse.getCards().stream()
                    .map(aiCard -> mapCardWithTopic(aiCard, topic.getId()))
                    .toList();

            // Update progress
            int completed = completedCounter.incrementAndGet();
            int progress = (completed * 100) / totalTopics;
            updateJobProgressAtomic(jobId, progress, completed);
            logger.info("Topic {} completed. Progress: {}/{} ({}%)",
                    topicConfig.getTopicId(), completed, totalTopics, progress);

            return cards;

        } catch (Exception e) {
            logger.error("Failed to process topic {}", topicConfig.getTopicId(), e);
            throw new RuntimeException("Failed to process topic: " + topicConfig.getTopicId(), e);
        }
    }

    private DeckPreviewResponse.GeneratedCard mapCardWithTopic(
            GenerateFlashcardAiResponse.Card aiCard,
            Long topicId
    ) {
        return DeckPreviewResponse.GeneratedCard.builder()
                .topicId(topicId)
                .frontText(aiCard.getFront_text())
                .backText(aiCard.getBack_text())
                .build();
    }

    protected void updateJobStatus(UUID jobId, JobStatus status) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            DeckPreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(status);
            jobRepository.save(job);
        });
    }

    protected synchronized void updateJobProgressAtomic(UUID jobId, Integer progress, Integer completed) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            DeckPreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setProgressPercent(progress);
            job.setCompletedTopics(completed);
            jobRepository.save(job);
        });
    }

    protected void updateJobCompleted(UUID jobId, String resultJson) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            DeckPreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedTopics(job.getTotalTopics());
            job.setResult(resultJson);
            jobRepository.save(job);
        });
    }

    protected void markJobFailed(UUID jobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(txStatus -> {
            DeckPreviewJob job = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            jobRepository.save(job);
        });
    }
}
