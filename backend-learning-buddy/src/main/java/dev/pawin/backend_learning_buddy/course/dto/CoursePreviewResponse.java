package dev.pawin.backend_learning_buddy.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoursePreviewResponse {
    private String courseTitle;
    private String description;
    private List<TopicPreviewDto> topics;
}
