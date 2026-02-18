package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckPreviewResponse {

    @JsonProperty("generated_cards")
    private List<GeneratedCard> generatedCards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedCard {
        @JsonProperty("topic_id")
        private Long topicId;

        @JsonProperty("front_text")
        private String frontText;

        @JsonProperty("back_text")
        private String backText;
    }
}
