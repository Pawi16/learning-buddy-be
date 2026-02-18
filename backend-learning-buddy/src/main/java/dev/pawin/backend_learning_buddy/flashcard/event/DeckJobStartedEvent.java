package dev.pawin.backend_learning_buddy.flashcard.event;

import dev.pawin.backend_learning_buddy.flashcard.dto.GenerateDeckRequest;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DeckJobStartedEvent {
    private final UUID jobId;
    private final GenerateDeckRequest request;

    public DeckJobStartedEvent(UUID jobId, GenerateDeckRequest request) {
        this.jobId = jobId;
        this.request = request;
    }
}
