package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.entity.AnswerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerHistoryRepository extends JpaRepository<AnswerHistory, Long> {
}
