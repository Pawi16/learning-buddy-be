package dev.pawin.backend_learning_buddy.course.service;

import dev.pawin.backend_learning_buddy.course.dto.CoursePreviewResponse;
import dev.pawin.backend_learning_buddy.course.dto.TopicPreviewDto;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final AiServiceClient aiServiceClient;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public CoursePreviewResponse previewCourse(String title, String description, MultipartFile file) {
        // Validate title
        if (title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be empty");
        }

        // Validate file (Optional)
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Check file Size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File is too large. Maximum allowed size is 10MB.");
        }

        // Check content type
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed. Received: " + file.getContentType());
        }

        // Call the AI Microservice
        List<TopicPreviewDto> topics = aiServiceClient.generateCoursePreview(file);

        // Construct the Response
        return CoursePreviewResponse.builder()
                .title(title)
                .topics(topics)
                .description(description)
                .build();
    }
}