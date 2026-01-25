package dev.pawin.backend_learning_buddy.course.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.dto.*;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.mapper.CourseMapper;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final AiServiceClient aiServiceClient;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
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

    @Transactional
    public CreateCourseResponse createCourse(CreateCourseRequest request, String username) {
        // Validate Uniqueness of order_index
        validateUniqueOrder((request.getTopics()));

        // Fetch Creator
        User creator = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Course course = courseMapper.toEntity(request);

        course.setCreator(creator);

        Course savedCourse = courseRepository.save(course);

        return CreateCourseResponse.builder()
                .courseId(savedCourse.getId())
                .message("Course created successfully.")
                .build();
    }

    private void validateUniqueOrder(List<TopicDraftDto> topics) {
        if (topics == null || topics.isEmpty()) return;

        Set<Integer> seenIndices = new HashSet<>();
        for (TopicDraftDto topic : topics) {
            // .add() returns false if the item was already in the Set
            if (!seenIndices.add(topic.getOrderIndex())) {
                throw new IllegalArgumentException(
                        "Duplicate topic order detected: Index " + topic.getOrderIndex() + " is used more than once."
                );
            }
        }
    }

    @Transactional()
    public List<CourseSummaryResponse> getPublicCourses(String search) {
        String searchPattern = null;
        // If search is empty string, treat it as null for the query logic
        if (search != null && !search.isBlank()) {
            // Prepare the pattern in Java: "%python%"
            // We also lowercase it here to ensure the parameter passed to DB is consistent
            searchPattern = "%" + search.toLowerCase().trim() + "%";
        }

        return courseRepository.searchPublicCourses(searchPattern);
    }
}