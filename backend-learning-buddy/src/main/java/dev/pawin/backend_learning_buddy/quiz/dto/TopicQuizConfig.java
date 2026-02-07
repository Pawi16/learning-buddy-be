package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
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
public class TopicQuizConfig {

    @NotNull(message = "Topic ID is required")
    @JsonProperty("topic_id")
    private Long topicId;

    @Valid
    @NotEmpty(message = "Quiz configuration is required")
    @JsonProperty("quiz_config")
    private List<QuizTypeConfig> quizConfig;
}
