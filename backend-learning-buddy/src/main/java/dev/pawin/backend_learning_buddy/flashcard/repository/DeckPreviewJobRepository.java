package dev.pawin.backend_learning_buddy.flashcard.repository;

import dev.pawin.backend_learning_buddy.flashcard.dto.DeckJobSummaryResponse;
import dev.pawin.backend_learning_buddy.flashcard.entity.DeckPreviewJob;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeckPreviewJobRepository extends JpaRepository<DeckPreviewJob, Long> {

    Optional<DeckPreviewJob> findByJobId(UUID jobId);

    @Query("SELECT new dev.pawin.backend_learning_buddy.flashcard.dto.DeckJobSummaryResponse(" +
           "j.jobId, c.id, c.title, j.status, j.progressPercent, " +
           "j.totalTopics, j.completedTopics, j.createdAt) " +
           "FROM DeckPreviewJob j JOIN j.course c " +
           "WHERE c.id = :courseId " +
           "AND (:status IS NULL OR j.status = :status)")
    List<DeckJobSummaryResponse> findJobSummariesByCourseId(
            @Param("courseId") Long courseId,
            @Param("status") JobStatus status
    );
}
