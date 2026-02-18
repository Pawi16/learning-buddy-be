CREATE TABLE deck_preview_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    progress_percent INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    total_topics INTEGER NOT NULL,
    completed_topics INTEGER NOT NULL DEFAULT 0,
    result JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deck_preview_jobs_job_id ON deck_preview_jobs(job_id);
CREATE INDEX idx_deck_preview_jobs_user_id ON deck_preview_jobs(user_id);
