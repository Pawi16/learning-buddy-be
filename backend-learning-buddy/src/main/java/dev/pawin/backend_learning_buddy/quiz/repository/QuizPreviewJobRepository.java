package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizJobSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizPreviewJobRepository extends JpaRepository<QuizPreviewJob, Long> {

    Optional<QuizPreviewJob> findByJobId(UUID jobId);

    List<QuizPreviewJob> findByStatus(JobStatus status);

    @Query("SELECT new dev.pawin.backend_learning_buddy.quiz.dto.QuizJobSummaryResponse(" +
           "j.jobId, c.id, c.title, j.status, j.progressPercent, " +
           "j.totalTopics, j.completedTopics, j.createdAt) " +
           "FROM QuizPreviewJob j JOIN j.course c " +
           "WHERE c.id = :courseId " +
           "AND (:status IS NULL OR j.status = :status)")
    List<QuizJobSummaryResponse> findJobSummariesByCourseId(
            @Param("courseId") Long courseId,
            @Param("status") JobStatus status
    );
}
