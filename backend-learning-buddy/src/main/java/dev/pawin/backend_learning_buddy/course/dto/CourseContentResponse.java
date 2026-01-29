package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseContentResponse {
    @JsonProperty("course_id")
    private Long id;

    private List<TopicDetailDto> topics;
}
