package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {

    @JsonProperty("attempt_id")
    private Long attemptId;

    @JsonProperty("total_score")
    private Integer totalScore;

    @JsonProperty("max_score")
    private Integer maxScore;

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    @JsonProperty("duration_seconds")
    private Long durationSeconds;

    private List<FeedbackDto> feedback;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackDto {
        @JsonProperty("question_id")
        private Long questionId;

        @JsonProperty("question_text")
        private String questionText;

        @JsonProperty("is_correct")
        private Boolean isCorrect;

        @JsonProperty("user_choice_ids")
        private List<Long> userChoiceIds;

        @JsonProperty("correct_choice_ids")
        private List<Long> correctChoiceIds;

        private String explanation;

        private List<ChoiceDetailDto> choices;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChoiceDetailDto {
            private Long id;
            @JsonProperty("choice_text")
            private String choiceText;
            private String explanation;
        }
    }
}
