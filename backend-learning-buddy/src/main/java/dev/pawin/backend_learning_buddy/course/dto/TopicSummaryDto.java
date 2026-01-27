package dev.pawin.backend_learning_buddy.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicSummaryDto {
    private Long topicId;
    private String title;
    private String description;
    private Integer orderIndex;
}
