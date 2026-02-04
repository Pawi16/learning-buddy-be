package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.dto.CourseSummaryResponse;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository <Course, Long> {

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.course.dto.CourseSummaryResponse(
            c.id,
            c.title,
            c.description,
            c.isPublished,
            COUNT(t),
            c.createdAt
        )
        FROM Course c
        LEFT JOIN c.topics t
        WHERE c.isPublished = true
        AND (:searchPattern IS NULL 
             OR LOWER(c.title) LIKE :searchPattern 
             OR LOWER(c.description) LIKE :searchPattern)
        GROUP BY c.id
    """)
    List<CourseSummaryResponse> searchPublicCourses(@Param("searchPattern") String searchPattern);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.topics WHERE c.id = :id")
    Optional<Course> findByIdWithTopics(@Param("id") Long id);

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.course.dto.CourseSummaryResponse(
            c.id,
            c.title,
            c.description,
            c.isPublished,
            COUNT(t),
            c.createdAt
        )
        FROM Course c
        LEFT JOIN c.topics t
        WHERE c.creator.id = :id
        GROUP BY c.id
    """)
    List<CourseSummaryResponse> findCoursesByCreatorId(@Param("id") Long id);

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.course.dto.CourseSummaryResponse(
            c.id,
            c.title,
            c.description,
            c.isPublished,
            COUNT(t),
            c.createdAt
        )
        FROM Enrollment e
        JOIN e.course c
        LEFT JOIN c.topics t
        WHERE e.user.username = :username
        GROUP BY c, e.createdAt
        ORDER BY e.createdAt DESC
    """)
    List<CourseSummaryResponse> findEnrolledCoursesByUsername(@Param("username") String username);
}
