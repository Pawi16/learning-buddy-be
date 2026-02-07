package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.DifficultyLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizTypeConfig {

    private DifficultyLevel difficulty;

    @Valid
    @NotEmpty(message = "At least one question type is required")
    @JsonProperty("quiz_type_config")
    private List<QuestionTypeConfig> types;
}
