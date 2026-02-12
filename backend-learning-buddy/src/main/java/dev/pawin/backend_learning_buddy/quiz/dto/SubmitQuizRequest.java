package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SubmitQuizRequest {

    @JsonProperty("answers")
    @NotEmpty(message = "Answers cannot be empty")
    private List<AnswerDto> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDto {
        @JsonProperty("question_id")
        private Long questionId;

        @JsonProperty("choice_id")
        private Long choiceId;
    }
}
