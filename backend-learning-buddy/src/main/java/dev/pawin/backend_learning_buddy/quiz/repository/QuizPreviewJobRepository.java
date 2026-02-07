package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizPreviewJobRepository extends JpaRepository<QuizPreviewJob, Long> {

    Optional<QuizPreviewJob> findByJobId(UUID jobId);

    List<QuizPreviewJob> findByStatus(JobStatus status);

    @Modifying
    @Query("UPDATE QuizPreviewJob j SET j.progressPercent = :progress, j.completedTopics = :completed WHERE j.id = :jobId")
    void updateProgressAtomic(@Param("jobId") Long jobId, @Param("progress") Integer progress, @Param("completed") Integer completed);
}
