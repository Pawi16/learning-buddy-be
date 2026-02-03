package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseDetailResponse {
    @JsonProperty("course_id")
    private Long courseId;
    private String title;
    private String description;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("is_enrolled")
    private Boolean isEnrolled;

    @JsonProperty("is_owner")
    private Boolean isOwner;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private List<TopicSummaryDto> topics;
}
