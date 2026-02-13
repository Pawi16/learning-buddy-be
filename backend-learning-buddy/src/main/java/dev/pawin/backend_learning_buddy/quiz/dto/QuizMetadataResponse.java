package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.SolutionVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizMetadataResponse {
    private Long id;

    @JsonProperty("course_id")
    private Long courseId;

    private String title;

    @JsonProperty("solution_visibility")
    private SolutionVisibility solutionVisibility;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
