package dev.pawin.backend_learning_buddy.quiz.mapper;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizResultResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.AnswerHistory;
import dev.pawin.backend_learning_buddy.quiz.entity.Choice;
import dev.pawin.backend_learning_buddy.quiz.entity.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuizResultMapperHelper {

    public List<QuizResultResponse.FeedbackDto> buildFeedback(
            List<AnswerHistory> answerHistories,
            List<Question> questions,
            boolean includeSolution) {

        if (!includeSolution) {
            return List.of();
        }

        // Create a map of question_id -> AnswerHistory for quick lookup
        Map<Long, AnswerHistory> answerMap = answerHistories.stream()
                .collect(Collectors.toMap(
                        ah -> ah.getQuestion().getId(),
                        ah -> ah
                ));

        List<QuizResultResponse.FeedbackDto> feedback = new ArrayList<>();

        for (Question question : questions) {
            AnswerHistory answerHistory = answerMap.get(question.getId());

            List<Long> correctChoiceIds = question.getChoices().stream()
                    .filter(Choice::getIsCorrect)
                    .map(Choice::getId)
                    .collect(Collectors.toList());

            List<QuizResultResponse.FeedbackDto.ChoiceDetailDto> choices = question.getChoices().stream()
                    .map(c -> QuizResultResponse.FeedbackDto.ChoiceDetailDto.builder()
                            .id(c.getId())
                            .choiceText(c.getChoiceText())
                            .build())
                    .collect(Collectors.toList());

            QuizResultResponse.FeedbackDto feedbackDto = QuizResultResponse.FeedbackDto.builder()
                    .questionId(question.getId())
                    .questionText(question.getQuestionText())
                    .isCorrect(answerHistory != null && correctChoiceIds.contains(answerHistory.getSelectedChoice().getId()))
                    .userChoiceIds(answerHistory != null
                            ? List.of(answerHistory.getSelectedChoice().getId())
                            : List.of())
                    .correctChoiceIds(correctChoiceIds)
                    .explanation(question.getExplanation())
                    .choices(choices)
                    .build();

            feedback.add(feedbackDto);
        }

        return feedback;
    }
}
