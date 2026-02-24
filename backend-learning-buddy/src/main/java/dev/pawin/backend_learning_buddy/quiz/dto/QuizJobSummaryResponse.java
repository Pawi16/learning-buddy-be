package dev.pawin.backend_learning_buddy.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class QuizJobSummaryResponse {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("course_id")
    private Long courseId;

    @JsonProperty("course_title")
    private String courseTitle;

    private String status;

    @JsonProperty("progress_percent")
    private Integer progressPercent;

    @JsonProperty("total_topics")
    private Integer totalTopics;

    @JsonProperty("completed_topics")
    private Integer completedTopics;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Constructor for JPQL - must match the exact types from the query
    public QuizJobSummaryResponse(
            UUID jobId,
            Long courseId,
            String courseTitle,
            JobStatus status,
            Integer progressPercent,
            Integer totalTopics,
            Integer completedTopics,
            LocalDateTime createdAt
    ) {
        this.jobId = jobId.toString();
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.status = status != null ? status.name() : null;
        this.progressPercent = progressPercent;
        this.totalTopics = totalTopics;
        this.completedTopics = completedTopics;
        this.createdAt = createdAt;
    }
}
