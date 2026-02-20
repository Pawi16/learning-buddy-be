package dev.pawin.backend_learning_buddy.flashcard.controller;

import dev.pawin.backend_learning_buddy.flashcard.dto.DeckDetailResponse;
import dev.pawin.backend_learning_buddy.flashcard.service.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckService deckService;

    @GetMapping("/{id}")
    public ResponseEntity<DeckDetailResponse> getDeckDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        DeckDetailResponse response = deckService.getDeckById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
