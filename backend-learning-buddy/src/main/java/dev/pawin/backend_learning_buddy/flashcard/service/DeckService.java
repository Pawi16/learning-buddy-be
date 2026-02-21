package dev.pawin.backend_learning_buddy.flashcard.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckRequest;
import dev.pawin.backend_learning_buddy.flashcard.dto.CreateDeckResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckDetailResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckMetadataResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckSummaryResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.UpdateDeckContentRequest;
import dev.pawin.backend_learning_buddy.flashcard.dto.UpdateDeckContentResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.UpdateDeckMetadataRequest;
import dev.pawin.backend_learning_buddy.flashcard.entity.Deck;
import dev.pawin.backend_learning_buddy.flashcard.entity.Flashcard;
import dev.pawin.backend_learning_buddy.flashcard.repository.DeckRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<DeckSummaryResponse> getDecksByCourseId(Long courseId, String username) {
        // Validate course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course not found with id: " + courseId));

        // Get current user
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if user is course owner
        boolean isOwner = course.getCreator().getId().equals(currentUser.getId());

        // Fetch deck summaries (owner sees all, others see only published)
        return deckRepository.findDeckSummariesByCourseId(courseId, isOwner);
    }

    @Transactional(readOnly = true)
    public DeckDetailResponse getDeckById(Long deckId, String username) {
        // 1. Fetch User
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2. Fetch Deck with Flashcards
        Deck deck = deckRepository.findDeckByIdWithFlashcards(deckId)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found"));

        // 3. Check if user is course owner
        boolean isOwner = deck.getCourse().getCreator().getId().equals(currentUser.getId());

        // 4. Access control: course owner can access unpublished decks, others can only access published decks
        if (!deck.getIsPublished() && !isOwner) {
            throw new EntityNotFoundException("Deck not found with id: " + deckId);
        }

        // 5. Map Flashcards to DTOs (topics are lazily loaded but accessible)
        List<DeckDetailResponse.CardDetailDto> cardDtos = deck.getFlashcards().stream()
                .map(flashcard -> DeckDetailResponse.CardDetailDto.builder()
                        .id(flashcard.getId())
                        .topicId(flashcard.getTopic().getId())
                        .frontText(flashcard.getFrontText())
                        .backText(flashcard.getBackText())
                        .build())
                .collect(Collectors.toList());

        // 6. Build and return response
        return DeckDetailResponse.builder()
                .deckId(deck.getId())
                .courseId(deck.getCourse().getId())
                .title(deck.getTitle())
                .isPublished(deck.getIsPublished())
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .cards(cardDtos)
                .build();
    }

    @Transactional
    public DeckMetadataResponse updateDeckMetadata(Long deckId, String username, UpdateDeckMetadataRequest request) {
        // 1. Fetch Deck
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found"));

        // 2. Fetch User and validate ownership (course creator only)
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 3. Validate Ownership (course creator only)
        if (!deck.getCourse().getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to edit this deck");
        }

        // 4. Update Title (always provided - required field with @NotBlank validation)
        deck.setTitle(request.getTitle());

        // 5. Partial Update: Is Published (optional, check if not null)
        if (request.getIsPublished() != null) {
            deck.setIsPublished(request.getIsPublished());
        }

        // 6. Save entity (updated_at is handled automatically by @PreUpdate in BaseEntity)
        Deck savedDeck = deckRepository.save(deck);

        // 7. Build and return DeckMetadataResponse
        return DeckMetadataResponse.builder()
                .id(savedDeck.getId())
                .courseId(savedDeck.getCourse().getId())
                .title(savedDeck.getTitle())
                .isPublished(savedDeck.getIsPublished())
                .createdAt(savedDeck.getCreatedAt())
                .updatedAt(savedDeck.getUpdatedAt())
                .build();
    }

    @Transactional
    public UpdateDeckContentResponse updateDeckContent(Long deckId, String username, UpdateDeckContentRequest request) {
        // 1. Fetch Deck with Flashcards (use existing query)
        Deck deck = deckRepository.findDeckByIdWithFlashcards(deckId)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found"));

        // 2. Fetch User and validate ownership (course creator only)
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!deck.getCourse().getCreator().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to update this deck content");
        }

        // 3. Build map of incoming cards with IDs (for O(1) lookup)
        Map<Long, UpdateDeckContentRequest.CardDetailDto> incomingCardMap = request.getCards().stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(
                        UpdateDeckContentRequest.CardDetailDto::getId, Function.identity()
                ));

        // 4. Process Existing Flashcards: Update or Delete
        Iterator<Flashcard> flashcardIterator = deck.getFlashcards().iterator();

        while (flashcardIterator.hasNext()) {
            Flashcard existingFlashcard = flashcardIterator.next();

            if (incomingCardMap.containsKey(existingFlashcard.getId())) {
                // UPDATE: Update flashcard fields
                UpdateDeckContentRequest.CardDetailDto incomingCard = incomingCardMap.get(existingFlashcard.getId());
                updateFlashcardFromDto(existingFlashcard, incomingCard);
                incomingCardMap.remove(existingFlashcard.getId());
            } else {
                // DELETE: Flashcard not in incoming payload → remove (orphanRemoval handles DB deletion)
                flashcardIterator.remove();
            }
        }

        // 5. Insert New Flashcards (those with null IDs)
        List<UpdateDeckContentRequest.CardDetailDto> newCards = request.getCards().stream()
                .filter(c -> c.getId() == null)
                .toList();

        for (UpdateDeckContentRequest.CardDetailDto cardDto : newCards) {
            // Fetch and validate topic
            Topic topic = courseRepository.findTopicById(cardDto.getTopicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Topic not found with id: " + cardDto.getTopicId()));

            // Verify topic belongs to the deck's course
            if (!topic.getCourse().getId().equals(deck.getCourse().getId())) {
                throw new IllegalArgumentException(
                        "Topic with id " + cardDto.getTopicId()
                                + " does not belong to course with id " + deck.getCourse().getId());
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

        // 6. Validation: If incomingCardMap is not empty, means user sent flashcard IDs belonging to other deck
        if (!incomingCardMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid Flashcard IDs provided (do not belong to this deck): " + incomingCardMap.keySet());
        }

        // 7. Save (cascade handles all CRUD operations, orphanRemoval deletes removed flashcards)
        deckRepository.save(deck);

        return UpdateDeckContentResponse.builder()
                .message("Deck content saved successfully.")
                .build();
    }

    private void updateFlashcardFromDto(Flashcard existingFlashcard, UpdateDeckContentRequest.CardDetailDto dto) {
        // Update text fields
        existingFlashcard.setFrontText(dto.getFrontText());
        existingFlashcard.setBackText(dto.getBackText());

        // Handle Topic update (validate belongs to same course)
        if (!existingFlashcard.getTopic().getId().equals(dto.getTopicId())) {
            Topic newTopic = courseRepository.findTopicById(dto.getTopicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Topic not found with id: " + dto.getTopicId()));

            if (!newTopic.getCourse().getId().equals(existingFlashcard.getDeck().getCourse().getId())) {
                throw new IllegalArgumentException(
                        "Topic with id " + dto.getTopicId()
                                + " does not belong to course with id " + existingFlashcard.getDeck().getCourse().getId());
            }
            existingFlashcard.setTopic(newTopic);
        }
    }
}
