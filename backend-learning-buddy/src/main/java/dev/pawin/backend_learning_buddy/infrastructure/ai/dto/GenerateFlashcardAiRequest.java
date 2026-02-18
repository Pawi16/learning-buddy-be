package dev.pawin.backend_learning_buddy.infrastructure.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFlashcardAiRequest {
    private String topicName;
    private String topicContent;
    private FlashcardConfig config;
}
