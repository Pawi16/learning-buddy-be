package dev.pawin.backend_learning_buddy.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStartResponse {

    private String jobId;
    private String status;
    private String message;
}
