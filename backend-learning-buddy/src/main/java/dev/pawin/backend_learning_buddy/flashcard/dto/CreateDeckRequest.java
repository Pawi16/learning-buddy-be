package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateDeckRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @JsonProperty("is_published")
    @Builder.Default
    private Boolean isPublished = false;

    @NotEmpty(message = "Deck must have at least one card")
    @Valid
    private List<CardDto> cards;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CardDto {

        @JsonProperty("topic_id")
        @NotNull(message = "Topic ID is required")
        private Long topicId;

        @JsonProperty("front_text")
        @NotBlank(message = "Front text is required")
        private String frontText;

        @JsonProperty("back_text")
        @NotBlank(message = "Back text is required")
        private String backText;
    }
}
