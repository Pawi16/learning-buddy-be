package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.id = :quizId")
    Optional<Quiz> findQuizByIdWithQuestions(@Param("quizId") Long quizId);

    @Query("""
        SELECT new dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse(
            q.id,
            q.title,
            q.isPublished,
            q.solutionVisibility,
            COUNT(qu),
            q.createdAt,
            q.updatedAt
        )
        FROM Quiz q
        LEFT JOIN q.questions qu
        WHERE q.course.id = :courseId
        AND (:isOwner = true OR q.isPublished = true)
        GROUP BY q.id
        ORDER BY q.createdAt DESC
    """)
    List<QuizSummaryResponse> findQuizSummariesByCourseId(@Param("courseId") Long courseId, @Param("isOwner") boolean isOwner);
}
