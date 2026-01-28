package dev.pawin.backend_learning_buddy.course.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.dto.*;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.mapper.CourseMapper;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.EnrollmentRepository;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import dev.pawin.backend_learning_buddy.infrastructure.ai.AiServiceClient;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final TopicRepository topicRepository;
    private final EnrollmentRepository enrollmentRepository;
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId, String username) {
        // Fetch User
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Fetch Course (Metadata)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // Check user access to this course
        // If not published, only the creator can see it
        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            if (!course.getCreator().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to view this private course.");
            }
        }

        // Get lightweight topic summaries
        List<TopicSummaryDto> topics = topicRepository.findSummariesByCourseId(courseId);

        // check if user enrolled this course (condition for showing enroll button)
        boolean isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(currentUser.getId(), courseId);

        return CourseDetailResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .isPublished(course.getIsPublished())
                .isEnrolled(isEnrolled)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .topics(topics)
                .build();
    }

    @Transactional
    public UpdateCourseResponse updateCourseMetadata(Long id, String username, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // fetch current user
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!course.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to edit this course.");
        }

        if (request.getTitle() != null){
            if (request.getTitle().isBlank()){
                throw new IllegalArgumentException("Title is required");
            }
            course.setTitle(request.getTitle());
        }

        if (request.getDescription() != null){
            course.setDescription(request.getDescription());
        }

        if (request.getIsPublished() != null){
            course.setIsPublished(request.getIsPublished());
        }

        CourseMetadataDto metadataResponse = courseMapper.toMetadataResponse(course);

        return UpdateCourseResponse.builder()
                .message("Course Update Successfully")
                .courseMetadataDto(metadataResponse)
                .build();
    }

    @Transactional
    public DeleteCourseResponse deleteCourse(Long id, String username) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // fetch current user & validate permission
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!course.getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this course.");
        }

        if (enrollmentRepository.existsByCourseId(id)) {
            throw new DataIntegrityViolationException("Cannot delete course with students.");
        }

        courseRepository.delete(course);

        return DeleteCourseResponse.builder().message("Course deleted successfully.").build();
    }
}