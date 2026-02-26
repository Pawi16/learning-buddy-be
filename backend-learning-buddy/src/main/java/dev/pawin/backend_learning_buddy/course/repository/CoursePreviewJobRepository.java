package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.dto.CourseJobSummaryResponse;
import dev.pawin.backend_learning_buddy.course.entity.CoursePreviewJob;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoursePreviewJobRepository extends JpaRepository<CoursePreviewJob, Long> {
    Optional<CoursePreviewJob> findByJobId(UUID jobId);

    @Query("SELECT new dev.pawin.backend_learning_buddy.course.dto.CourseJobSummaryResponse(" +
           "j.jobId, j.title, j.description, j.status, j.createdAt, j.updatedAt) " +
           "FROM CoursePreviewJob j " +
           "WHERE j.user.username = :username " +
           "AND (:status IS NULL OR j.status = :status) " +
           "ORDER BY j.createdAt DESC")
    List<CourseJobSummaryResponse> findJobSummariesByUsername(
            @Param("username") String username,
            @Param("status") JobStatus status
    );
}
