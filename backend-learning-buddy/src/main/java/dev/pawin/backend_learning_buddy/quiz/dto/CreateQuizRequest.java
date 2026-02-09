package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.DifficultyLevel;
import dev.pawin.backend_learning_buddy.common.enumeration.QuestionType;
import dev.pawin.backend_learning_buddy.common.enumeration.SolutionVisibility;
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
public class CreateQuizRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @JsonProperty("solution_visibility")
    @Builder.Default
    private SolutionVisibility solutionVisibility = SolutionVisibility.ALWAYS;

    @JsonProperty("is_published")
    @Builder.Default
    private Boolean isPublished = false;

    @NotEmpty(message = "Quiz must have at least one question")
    @Valid
    private List<QuestionDto> questions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuestionDto {

        @JsonProperty("topic_id")
        @NotNull(message = "Topic ID is required")
        private Long topicId;

        @JsonProperty("question_text")
        @NotBlank(message = "Question text is required")
        private String questionText;

        @JsonProperty("question_type")
        @Builder.Default
        private QuestionType questionType = QuestionType.NORMAL_MULTIPLE;

        @Builder.Default
        private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

        private String explanation;

        @NotEmpty(message = "Question must have at least one choice")
        @Valid
        private List<ChoiceDto> choices;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChoiceDto {

        @JsonProperty("choice_text")
        @NotBlank(message = "Choice text is required")
        private String choiceText;

        @JsonProperty("is_correct")
        @NotNull(message = "isCorrect is required")
        private Boolean isCorrect;
    }
}
