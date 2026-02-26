package dev.pawin.backend_learning_buddy.course.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class CourseJobStartedEvent {
    private final UUID jobId;
    private final String title;
    private final String description;
    private final String filename;
    private final String contentType;
    private final byte[] fileContent;

    public CourseJobStartedEvent(UUID jobId, String title, String description,
                                 String filename, String contentType, byte[] fileContent) {
        this.jobId = jobId;
        this.title = title;
        this.description = description;
        this.filename = filename;
        this.contentType = contentType;
        this.fileContent = fileContent;
    }
}
