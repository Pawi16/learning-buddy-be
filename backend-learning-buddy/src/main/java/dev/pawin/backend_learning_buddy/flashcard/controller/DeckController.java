package dev.pawin.backend_learning_buddy.flashcard.controller;

import dev.pawin.backend_learning_buddy.flashcard.dto.DeckDetailResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.DeckMetadataResponse;
import dev.pawin.backend_learning_buddy.flashcard.dto.UpdateDeckMetadataRequest;
import dev.pawin.backend_learning_buddy.flashcard.service.DeckService;
import jakarta.validation.Valid;
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

    @PatchMapping("/{id}")
    public ResponseEntity<DeckMetadataResponse> updateDeckMetadata(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeckMetadataRequest request,
            Authentication authentication
    ) {
        DeckMetadataResponse response = deckService.updateDeckMetadata(
                id, authentication.getName(), request
        );
        return ResponseEntity.ok(response);
    }
}
