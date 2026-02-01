package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDetailDto
{
    private Long id;

    @NotNull(message = "Order index is required")
    @JsonProperty("order_index")
    private Integer orderIndex;

    @NotBlank(message = "Topic title is required")
    private String title;

    private String description;

    @NotBlank(message = "Raw text is required")
    @JsonProperty("raw_text")
    private String rawText;

    @NotBlank(message = "Summary note is required")
    @JsonProperty("summary_note")
    private String summaryNote;
}
