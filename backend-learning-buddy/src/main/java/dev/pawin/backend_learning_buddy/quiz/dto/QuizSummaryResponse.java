package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.SolutionVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSummaryResponse {
    @JsonProperty("quiz_id")
    private Long quizId;

    private String title;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("solution_visibility")
    private SolutionVisibility solutionVisibility;

    @JsonProperty("question_count")
    private Long questionCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
