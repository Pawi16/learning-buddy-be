package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.dto.TopicDetailDto;
import dev.pawin.backend_learning_buddy.course.dto.TopicSummaryDto;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.course.dto.TopicSummaryDto(
            t.id, t.title, t.description, t.orderIndex
        )
        FROM Topic t
        WHERE t.course.id = :courseId
        ORDER BY t.orderIndex ASC
    """)
    List<TopicSummaryDto> findSummariesByCourseId(Long courseId);

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.course.dto.TopicDetailDto(
            t.id, t.orderIndex, t.title, t.description, t.rawText, t.summaryNote
        )
        FROM Topic t
        WHERE t.course.id = :courseId
        ORDER BY t.orderIndex ASC
    """)
    List<TopicDetailDto> findDetailsByCourseId(Long courseId);
}
