package dev.pawin.backend_learning_buddy.quiz.repository;

import dev.pawin.backend_learning_buddy.quiz.entity.Choice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChoiceRepository extends JpaRepository<Choice, Long> {

    @Query("SELECT c FROM Choice c WHERE c.question.id IN :questionIds")
    List<Choice> findChoicesByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
