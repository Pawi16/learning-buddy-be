package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.entity.CoursePreviewJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoursePreviewJobRepository extends JpaRepository<CoursePreviewJob, Long> {
    Optional<CoursePreviewJob> findByJobId(UUID jobId);
}
