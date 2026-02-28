package dev.pawin.backend_learning_buddy.quiz.dto;

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
public class QuizPreviewResponse {

    @JsonProperty("generated_questions")
    private List<GeneratedQuestion> generatedQuestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedQuestion {
        @JsonProperty("topic_id")
        private Long topicId;

        @JsonProperty("question_text")
        private String questionText;

        @JsonProperty("question_type")
        private String questionType;

        @JsonProperty("difficulty")
        private String difficulty;

        private String explanation;

        private List<Choice> choices;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            @JsonProperty("choice_text")
            private String choiceText;

            @JsonProperty("is_correct")
            private Boolean isCorrect;

            private String explanation;
        }
    }
}
