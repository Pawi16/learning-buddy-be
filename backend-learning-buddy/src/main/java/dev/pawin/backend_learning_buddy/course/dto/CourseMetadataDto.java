package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseMetadataDto {
    private Long id;
    private String title;
    private String description;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
