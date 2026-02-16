package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.entity.QuizPreviewJob;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizPreviewJobRepository extends JpaRepository<QuizPreviewJob, Long> {

    Optional<QuizPreviewJob> findByJobId(UUID jobId);

    List<QuizPreviewJob> findByStatus(JobStatus status);
}
