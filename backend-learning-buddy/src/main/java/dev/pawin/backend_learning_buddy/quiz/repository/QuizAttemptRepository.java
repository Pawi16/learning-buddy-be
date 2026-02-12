package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.common.enumeration.AttemptStatus;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findByUserIdAndQuizIdAndStatus(Long userId, Long quizId, AttemptStatus status);
}
