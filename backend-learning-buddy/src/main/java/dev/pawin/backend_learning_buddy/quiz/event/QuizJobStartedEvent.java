package dev.pawin.backend_learning_buddy.quiz.event;

import dev.pawin.backend_learning_buddy.quiz.dto.GenerateQuizRequest;
import lombok.Getter;

import java.util.UUID;

@Getter
public class QuizJobStartedEvent {
    private final UUID jobId;
    private final GenerateQuizRequest request;

    public QuizJobStartedEvent(UUID jobId, GenerateQuizRequest request) {
        this.jobId = jobId;
        this.request = request;
    }
}
