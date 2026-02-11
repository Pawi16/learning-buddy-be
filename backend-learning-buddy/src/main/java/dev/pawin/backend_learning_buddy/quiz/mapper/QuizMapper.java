package dev.pawin.backend_learning_buddy.quiz.mapper;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse.ChoiceExamDto;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse.QuestionExamDto;
import dev.pawin.backend_learning_buddy.quiz.entity.Choice;
import dev.pawin.backend_learning_buddy.quiz.entity.Question;
import dev.pawin.backend_learning_buddy.quiz.entity.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuizMapper {

    @Mapping(target = "quizId", source = "id")
    @Mapping(target = "courseId", source = "course.id")
    QuizExamDetailResponse toQuizExamDetailResponse(Quiz quiz);

    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "questionType", expression = "java(question.getQuestionType().name())")
    @Mapping(target = "difficulty", expression = "java(question.getDifficultyLevel() != null ? question.getDifficultyLevel().name() : null)")
    QuestionExamDto toQuestionExamDto(Question question);

    ChoiceExamDto toChoiceExamDto(Choice choice);
}
