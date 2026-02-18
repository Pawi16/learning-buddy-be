package dev.pawin.backend_learning_buddy.infrastructure.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFlashcardAiResponse {
    private String topic_title;
    private List<Card> cards;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Card {
        private Integer id;
        private String front_text;
        private String back_text;
        private String category;
    }
}
