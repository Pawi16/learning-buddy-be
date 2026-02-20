package dev.pawin.backend_learning_buddy.flashcard.repository;

import dev.pawin.backend_learning_buddy.flashcard.dto.DeckSummaryResponse;
import dev.pawin.backend_learning_buddy.flashcard.entity.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.flashcard.dto.DeckSummaryResponse(
            d.id,
            d.title,
            d.isPublished,
            COUNT(f),
            d.createdAt,
            d.updatedAt
        )
        FROM Deck d
        LEFT JOIN d.flashcards f
        WHERE d.course.id = :courseId
        AND (:isOwner = true OR d.isPublished = true)
        GROUP BY d.id
        ORDER BY d.createdAt DESC
    """)
    List<DeckSummaryResponse> findDeckSummariesByCourseId(@Param("courseId") Long courseId, @Param("isOwner") boolean isOwner);
}
