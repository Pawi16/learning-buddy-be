package dev.pawin.backend_learning_buddy.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCoursePreviewRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "File is required")
    private MultipartFile file;
}
