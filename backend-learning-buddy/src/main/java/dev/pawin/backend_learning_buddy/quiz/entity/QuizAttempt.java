package dev.pawin.backend_learning_buddy.quiz.entity;

import dev.pawin.backend_learning_buddy.common.entity.BaseEntity;
import dev.pawin.backend_learning_buddy.common.enumeration.AttemptStatus;
import dev.pawin.backend_learning_buddy.auth.entity.User;
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
@Table(name = "quiz_attempts")
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "quiz_score")
    private Integer quizScore;

    @Column(name = "start_time")
    private java.time.LocalDateTime startTime;

    @Column(name = "end_time")
    private java.time.LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status; // IN_PROGRESS, COMPLETED

    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnswerHistory> answerHistories = new ArrayList<>();

}
