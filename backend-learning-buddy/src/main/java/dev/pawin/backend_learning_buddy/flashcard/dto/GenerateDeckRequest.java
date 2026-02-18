package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDeckRequest {

    @Valid
    @NotEmpty(message = "At least one topic is required")
    @Size(max = 10, message = "Cannot generate deck for more than 10 topics at once")
    @JsonProperty("deck_topics")
    private List<DeckTopicConfig> deckTopics;
}
