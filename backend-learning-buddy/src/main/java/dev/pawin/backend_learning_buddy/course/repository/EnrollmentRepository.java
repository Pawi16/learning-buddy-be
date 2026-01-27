package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
