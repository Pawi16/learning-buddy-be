-- Add course_id column to deck_preview_jobs table
ALTER TABLE deck_preview_jobs
ADD COLUMN course_id BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE deck_preview_jobs
ADD CONSTRAINT fk_deck_preview_job_course
FOREIGN KEY (course_id) REFERENCES courses(id);

-- Create index for better query performance
CREATE INDEX idx_deck_preview_jobs_course_id ON deck_preview_jobs(course_id);
