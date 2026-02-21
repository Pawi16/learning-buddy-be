package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeckMetadataRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @JsonProperty("is_published")
    private Boolean isPublished;
}
