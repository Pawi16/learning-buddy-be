package dev.pawin.backend_learning_buddy.quiz.dto;

import dev.pawin.backend_learning_buddy.common.enumeration.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTypeConfig {

    @NotNull(message = "Question type is required")
    private QuestionType type;

    @NotNull(message = "Number of questions is required")
    @Min(value = 1, message = "Number of questions must be at least 1")
    @Max(value = 10, message = "Number of questions cannot exceed 10")
    private Integer number;
}
