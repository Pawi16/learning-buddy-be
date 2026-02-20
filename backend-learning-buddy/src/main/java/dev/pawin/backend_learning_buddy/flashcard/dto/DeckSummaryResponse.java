package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeckSummaryResponse {
    @JsonProperty("deck_id")
    private Long deckId;

    private String title;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("card_count")
    private Long cardCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
