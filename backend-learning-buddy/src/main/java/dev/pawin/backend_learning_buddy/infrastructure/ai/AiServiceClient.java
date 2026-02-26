package dev.pawin.backend_learning_buddy.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.pawin.backend_learning_buddy.common.exception.AiServiceException;
import dev.pawin.backend_learning_buddy.course.dto.TopicPreviewDto;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.AiErrorResponse;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateQuizAiRequest;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateQuizAiResponse;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateFlashcardAiRequest;
import dev.pawin.backend_learning_buddy.infrastructure.ai.dto.GenerateFlashcardAiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class AiServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);
    private final String aiServiceUrl;

    public AiServiceClient(
            @Value("${application.ai-service.url}") String aiServiceUrl
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds to find the server
        factory.setReadTimeout(600000);  // 10 minutes (600000ms) for AI processing

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        this.aiServiceUrl = aiServiceUrl;
        this.objectMapper = new ObjectMapper();
    }

    public List<TopicPreviewDto> generateCoursePreview(String filename, String contentType, byte[] fileContent) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", fileContent)
                .filename(filename)
                .contentType(MediaType.parseMediaType(contentType));


        try {
            return restClient.post()
                    .uri(aiServiceUrl + "/api/v1/process-pdf")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TopicPreviewDto>>() {});

        } catch (RestClientResponseException e) {
            // Case 1: The AI Service replied with an Error JSON (400, 413, 500, etc.)
            // We parse the JSON to get the real message (e.g., "File too large")
            String friendlyMessage = extractErrorMessage(e);
            logger.error("AI Service Error: {}", friendlyMessage);
            throw new AiServiceException(friendlyMessage, e);

        } catch (ResourceAccessException e) {
            // Case 2: The AI Service is down or unreachable
            logger.error("AI Service is down");
            throw new AiServiceException("AI Service is currently unavailable. Please try again later.", e);
        }
    }

    private String extractErrorMessage(RestClientResponseException e) {
        try {
            // Attempt to parse the JSON body into our DTO
            AiErrorResponse errorResponse = objectMapper.readValue(
                    e.getResponseBodyAsString(),
                    AiErrorResponse.class
            );

            // If parsing works, return the clean 'message' field
            // e.g. "File too large. Maximum size: 10.0MB"
            return errorResponse.message();

        } catch (Exception parseException) {
            // If the response wasn't JSON (e.g., a raw Nginx HTML error), fall back to status code
            return "AI Service Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
        }
    }

    public GenerateQuizAiResponse generateQuiz(
            String topicName,
            String topicContent,
            List<GenerateQuizAiRequest.QuizConfig> quizConfig
    ) {
        GenerateQuizAiRequest request = GenerateQuizAiRequest.builder()
                .topicName(topicName)
                .topicContent(topicContent)
                .quizConfig(quizConfig)
                .build();

        try {
            return restClient.post()
                    .uri(aiServiceUrl + "/api/v1/generate-quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateQuizAiResponse.class);

        } catch (RestClientResponseException e) {
            String friendlyMessage = extractErrorMessage(e);
            logger.error("AI Service Quiz Generation Error: {}", friendlyMessage);
            throw new AiServiceException(friendlyMessage, e);

        } catch (ResourceAccessException e) {
            logger.error("AI Service is down during quiz generation");
            throw new AiServiceException("AI Service is currently unavailable. Please try again later.", e);
        }
    }

    public GenerateFlashcardAiResponse generateFlashcard(
            String topicName,
            String topicContent,
            Integer amount
    ) {
        dev.pawin.backend_learning_buddy.infrastructure.ai.dto.FlashcardConfig config =
                dev.pawin.backend_learning_buddy.infrastructure.ai.dto.FlashcardConfig.builder()
                        .amount(amount)
                        .build();

        GenerateFlashcardAiRequest request = GenerateFlashcardAiRequest.builder()
                .topicName(topicName)
                .topicContent(topicContent)
                .config(config)
                .build();

        try {
            return restClient.post()
                    .uri(aiServiceUrl + "/api/v1/generate-flashcard")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateFlashcardAiResponse.class);

        } catch (RestClientResponseException e) {
            String friendlyMessage = extractErrorMessage(e);
            logger.error("AI Service Flashcard Generation Error: {}", friendlyMessage);
            throw new AiServiceException(friendlyMessage, e);

        } catch (ResourceAccessException e) {
            logger.error("AI Service is down during flashcard generation");
            throw new AiServiceException("AI Service is currently unavailable. Please try again later.", e);
        }
    }
}