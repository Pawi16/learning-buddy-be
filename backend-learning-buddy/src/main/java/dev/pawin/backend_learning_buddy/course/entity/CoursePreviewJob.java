package dev.pawin.backend_learning_buddy.course.entity;

import dev.pawin.backend_learning_buddy.common.entity.BaseEntity;
import dev.pawin.backend_learning_buddy.common.enumeration.JobStatus;
import dev.pawin.backend_learning_buddy.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_preview_jobs")
public class CoursePreviewJob extends BaseEntity {

    @Column(name = "job_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "result", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String result;

}
