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
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeckContentRequest {

    @NotEmpty(message = "Deck must have at least one card")
    @Valid
    private List<CardDetailDto> cards;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardDetailDto {
        private Long id; // null for new, existing ID for update

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
