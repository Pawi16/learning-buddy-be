package dev.pawin.backend_learning_buddy.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.DifficultyLevel;
import dev.pawin.backend_learning_buddy.common.enumeration.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuizAiRequest {

    private String topicName;
    private String topicContent;
    @JsonProperty("quiz_config")
    private List<QuizConfig> quizConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizConfig {
        private DifficultyLevel difficulty;
        @JsonProperty("quiz_type_config")
        private List<QuizTypeConfig> quizTypeConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizTypeConfig {
        private QuestionType type;
        private Integer number;
    }
}
