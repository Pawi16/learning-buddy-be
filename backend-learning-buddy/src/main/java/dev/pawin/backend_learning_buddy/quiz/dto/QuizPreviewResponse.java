package dev.pawin.backend_learning_buddy.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizPreviewResponse {

    private List<TopicQuizPreview> topics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicQuizPreview {
        private Long topicId;
        private String topicTitle;
        private List<QuestionPreview> questions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionPreview {
        private String questionText;
        private String questionType;
        private String difficultyLevel;
        private String explanation;
        private List<ChoicePreview> choices;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoicePreview {
        private String choiceText;
        private Boolean isCorrect;
    }
}
