package dev.pawin.backend_learning_buddy.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeckJobStatusResponse {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("course_id")
    private Long courseId;

    @JsonProperty("course_title")
    private String courseTitle;

    private String status;

    @JsonProperty("progress_percent")
    private Integer progressPercent;

    @JsonProperty("error_message")
    private String errorMessage;

    private DeckPreviewResponse result;
}
