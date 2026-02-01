package dev.pawin.backend_learning_buddy.course.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCourseContentRequest {

    @NotEmpty(message = "Course must have at least one topic")
    @Valid
    List<TopicDetailDto> topics;
}
