package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseResponse {
    @JsonProperty("data")
    private CourseMetadataDto courseMetadataDto;
    private String message;
}
