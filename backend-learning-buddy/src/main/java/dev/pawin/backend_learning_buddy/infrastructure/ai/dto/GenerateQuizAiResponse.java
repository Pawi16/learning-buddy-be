package dev.pawin.backend_learning_buddy.infrastructure.ai.dto;

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
public class GenerateQuizAiResponse {

    private List<Question> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        @JsonProperty("question_text")
        private String questionText;
        @JsonProperty("question_type")
        private String questionType;
        @JsonProperty("difficulty_level")
        private String difficultyLevel;
        private String explanation;
        private List<Choice> choices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        @JsonProperty("choice_text")
        private String choiceText;
        @JsonProperty("is_correct")
        private Boolean isCorrect;
    }
}
