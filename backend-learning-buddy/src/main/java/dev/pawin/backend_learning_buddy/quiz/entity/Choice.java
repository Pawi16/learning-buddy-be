package dev.pawin.backend_learning_buddy.quiz.entity;

import dev.pawin.backend_learning_buddy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "choices")
public class Choice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "choice_text", nullable = false)
    private String choiceText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "explanation")
    private String explanation;

    @OneToMany(mappedBy = "selectedChoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerHistory> answerHistories = new ArrayList<>();

}
