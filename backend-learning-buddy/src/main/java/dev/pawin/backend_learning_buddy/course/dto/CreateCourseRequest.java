package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCourseRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Builder.Default
    @NotNull(message = "isPublished is required")
    @JsonProperty("is_published")
    private Boolean isPublished = true;


    @NotEmpty(message = "Course must have at least one topic")
    @Valid
    private List<TopicDraftDto> topics;

}
