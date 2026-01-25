package dev.pawin.backend_learning_buddy.course.mapper;

import dev.pawin.backend_learning_buddy.course.dto.CreateCourseRequest;
import dev.pawin.backend_learning_buddy.course.dto.TopicDraftDto;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "quizzes", ignore = true)
    @Mapping(target = "decks", ignore = true)
    Course toEntity(CreateCourseRequest request);

    @Mapping(target = "course", ignore = true) // Cannot map parent here yet
    @Mapping(target = "flashcards", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "topicProgresses", ignore = true)
    Topic toTopicEntity(TopicDraftDto dto);

    @AfterMapping
    default void linkTopics(@MappingTarget Course course) {
        if (course.getTopics() != null) {
            // "this" refers to the Course entity being created
            course.getTopics().forEach(topic -> topic.setCourse(course));
        }
    }
}
