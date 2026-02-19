package dev.pawin.backend_learning_buddy.flashcard.repository;

import dev.pawin.backend_learning_buddy.flashcard.entity.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, Long> {
}
