package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckDetailResponse {
    @JsonProperty("deck_id")
    private Long deckId;

    @JsonProperty("course_id")
    private Long courseId;

    private String title;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private List<CardDetailDto> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDetailDto {
        private Long id;

        @JsonProperty("topic_id")
        private Long topicId;

        @JsonProperty("front_text")
        private String frontText;

        @JsonProperty("back_text")
        private String backText;
    }
}
