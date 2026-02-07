package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuizRequest {

    @Valid
    @NotEmpty(message = "At least one topic is required")
    @Size(max = 10, message = "Cannot generate quiz for more than 10 topics at once")
    @JsonProperty("quiz_topics")
    private List<TopicQuizConfig> quizTopics;
}
