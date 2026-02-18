package dev.pawin.backend_learning_buddy.flashcard.repository;

import dev.pawin.backend_learning_buddy.flashcard.entity.DeckPreviewJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeckPreviewJobRepository extends JpaRepository<DeckPreviewJob, Long> {

    Optional<DeckPreviewJob> findByJobId(UUID jobId);

}
