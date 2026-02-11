package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.SolutionVisibility;
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
public class QuizExamDetailResponse {
    @JsonProperty("quiz_id")
    private Long quizId;

    @JsonProperty("course_id")
    private Long courseId;

    private String title;

    @JsonProperty("solution_visibility")
    private SolutionVisibility solutionVisibility;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private List<QuestionExamDto> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionExamDto {
        private Long id;

        @JsonProperty("topic_id")
        private Long topicId;

        @JsonProperty("question_text")
        private String questionText;

        @JsonProperty("question_type")
        private String questionType;

        private String difficulty;

        private List<ChoiceExamDto> choices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceExamDto {
        private Long id;

        @JsonProperty("choice_text")
        private String choiceText;

    }
}
