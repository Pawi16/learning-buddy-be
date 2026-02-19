package dev.pawin.backend_learning_buddy.flashcard.service;

import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckResponse;
import dev.pawin.backend_learning_buddy.flashcard.entity.Deck;
import dev.pawin.backend_learning_buddy.flashcard.entity.Flashcard;
import dev.pawin.backend_learning_buddy.flashcard.repository.DeckRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CreateDeckResponse createDeck(Long courseId, CreateDeckRequest request, String username) {
        // Fetch and validate course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course not found with id: " + courseId));

        // Check user permission (must be course creator)
        if (!course.getCreator().getUsername().equals(username)) {
            throw new AccessDeniedException(
                    "You do not have permission to create decks for this course.");
        }

        // Validate title (defensive check, already validated by @NotBlank)
        if (request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Deck title cannot be blank");
        }

        // Validate cards not empty (defensive check, already validated by @NotEmpty)
        if (request.getCards().isEmpty()) {
            throw new IllegalArgumentException("Deck must have at least one card");
        }

        // Build Deck entity
        Deck deck = Deck.builder()
                .course(course)
                .title(request.getTitle())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .flashcards(new ArrayList<>())
                .build();

        // Build Flashcard entities with bidirectional relationships
        for (CreateDeckRequest.CardDto cardDto : request.getCards()) {
            // Fetch and validate topic
            Topic topic = courseRepository.findTopicById(cardDto.getTopicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Topic not found with id: " + cardDto.getTopicId()));

            // Verify topic belongs to the specified course
            if (!topic.getCourse().getId().equals(courseId)) {
                throw new IllegalArgumentException(
                        "Topic with id " + cardDto.getTopicId()
                                + " does not belong to course with id " + courseId);
            }

            // Build Flashcard entity
            Flashcard flashcard = Flashcard.builder()
                    .deck(deck)
                    .topic(topic)
                    .frontText(cardDto.getFrontText())
                    .backText(cardDto.getBackText())
                    .build();

            // Add flashcard to deck (establishes bidirectional relationship)
            deck.getFlashcards().add(flashcard);
        }

        // Save deck (cascade will save flashcards)
        Deck savedDeck = deckRepository.save(deck);

        // Build and return response
        return CreateDeckResponse.builder()
                .deckId(savedDeck.getId())
                .message("Deck created successfully")
                .build();
    }
}
