package dev.pawin.backend_learning_buddy.course.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.dto.EnrollCourseResponse;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Enrollment;
import dev.pawin.backend_learning_buddy.course.mapper.CourseMapper;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.EnrollmentRepository;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public EnrollCourseResponse enrollUser(Long courseId, String username) {
        // Fetch User
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Fetch Course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // Validation: Cannot enroll in unpublished courses
        if (!Boolean.TRUE.equals(course.getIsPublished())) {
            throw new IllegalArgumentException("Cannot enroll in a course that is not published.");
        }

        // Validation: Creator cannot enroll in their own course
        if (course.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot enroll in your own course. Use 'Instructor View' instead.");
        }

        // Validation: Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new IllegalStateException("You are already enrolled in this course.");
        }

        // Create & Save Enrollment
        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .build();

        enrollmentRepository.save(enrollment);
        return EnrollCourseResponse.builder().message("Enrollment successful.").build();
    }
}
