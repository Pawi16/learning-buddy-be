-- Quiz Preview Jobs Table for asynchronous quiz generation
CREATE TABLE quiz_preview_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    total_topics INTEGER NOT NULL,
    completed_topics INTEGER NOT NULL DEFAULT 0,
    result JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quiz_preview_job_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_quiz_preview_jobs_job_id ON quiz_preview_jobs(job_id);
CREATE INDEX idx_quiz_preview_jobs_user_id ON quiz_preview_jobs(user_id);
CREATE INDEX idx_quiz_preview_jobs_status ON quiz_preview_jobs(status);
