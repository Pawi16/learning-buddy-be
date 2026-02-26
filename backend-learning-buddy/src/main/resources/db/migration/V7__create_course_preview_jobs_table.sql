CREATE TABLE course_preview_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    result JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_course_preview_job_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_course_preview_jobs_job_id ON course_preview_jobs(job_id);
CREATE INDEX idx_course_preview_jobs_user_id ON course_preview_jobs(user_id);
CREATE INDEX idx_course_preview_jobs_status ON course_preview_jobs(status);
