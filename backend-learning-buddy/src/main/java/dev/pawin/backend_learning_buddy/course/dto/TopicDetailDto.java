package dev.pawin.backend_learning_buddy.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDetailDto
{
    private Long id;

    @JsonProperty("order_index")
    private Integer orderIndex;

    private String title;

    private String description;

    @JsonProperty("raw_text")
    private String rawText;

    @JsonProperty("summary_note")
    private String summaryNote;
}
