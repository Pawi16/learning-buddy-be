package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CourseJobSummaryResponse {

    @JsonProperty("job_id")
    private String jobId;

    private String title;
    private String description;
    private String status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Constructor for JPQL
    public CourseJobSummaryResponse(
            UUID jobId,
            String title,
            String description,
            JobStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.jobId = jobId.toString();
        this.title = title;
        this.description = description;
        this.status = status != null ? status.name() : null;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
