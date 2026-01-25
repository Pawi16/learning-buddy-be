package dev.pawin.backend_learning_buddy.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private Boolean isPublished;
    private Long totalTopics;    // JPQL count() returns Long
    private LocalDateTime createdAt;
}
