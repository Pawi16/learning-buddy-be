package dev.pawin.backend_learning_buddy.quiz.mapper;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizResultResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizAttempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = QuizResultMapperHelper.class)
public interface QuizResultMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Mapping(target = "attemptId", source = "quizAttempt.id")
    @Mapping(target = "totalScore", source = "quizAttempt.quizScore")
    @Mapping(target = "maxScore", source = "maxScore")
    @Mapping(target = "startTime", expression = "java(formatDateTime(quizAttempt.getStartTime()))")
    @Mapping(target = "endTime", expression = "java(formatDateTime(quizAttempt.getEndTime()))")
    @Mapping(target = "durationSeconds", source = "durationSeconds")
    @Mapping(target = "feedback", source = "feedback")
    QuizResultResponse toQuizResultResponse(QuizAttempt quizAttempt, Integer maxScore, Long durationSeconds, List<QuizResultResponse.FeedbackDto> feedback);

    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).format(FORMATTER);
    }
}
