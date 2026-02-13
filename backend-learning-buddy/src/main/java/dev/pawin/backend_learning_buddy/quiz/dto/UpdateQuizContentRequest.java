package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.DifficultyLevel;
import dev.pawin.backend_learning_buddy.common.enumeration.QuestionType;
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
public class UpdateQuizContentRequest {

    @NotEmpty(message = "Quiz must have at least one question")
    @Valid
    private List<QuestionDetailDto> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDetailDto {
        private Long id; // null for new, existing ID for update

        @JsonProperty("question_text")
        @NotBlank(message = "Question text is required")
        private String questionText;

        @JsonProperty("question_type")
        private QuestionType questionType;

        private DifficultyLevel difficulty;

        @JsonProperty("topic_id")
        @NotNull(message = "Topic ID is required")
        private Long topicId;

        private String explanation;

        @NotEmpty(message = "Question must have at least one choice")
        @Valid
        private List<ChoiceDetailDto> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceDetailDto {
        private Long id; // null for new, existing ID for update

        @JsonProperty("choice_text")
        @NotBlank(message = "Choice text is required")
        private String choiceText;

        @JsonProperty("is_correct")
        @NotNull(message = "isCorrect is required")
        private Boolean isCorrect;
    }
}
