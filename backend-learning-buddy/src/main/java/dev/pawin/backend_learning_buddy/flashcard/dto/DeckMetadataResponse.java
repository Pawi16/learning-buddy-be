package dev.pawin.backend_learning_buddy.flashcard.dto;

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
public class DeckMetadataResponse {
    private Long id;

    @JsonProperty("course_id")
    private Long courseId;

    private String title;

    @JsonProperty("is_published")
    private Boolean isPublished;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
