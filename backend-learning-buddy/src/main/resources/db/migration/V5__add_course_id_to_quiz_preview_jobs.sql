-- Add course_id column to quiz_preview_jobs table
ALTER TABLE quiz_preview_jobs
ADD COLUMN course_id BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE quiz_preview_jobs
ADD CONSTRAINT fk_quiz_preview_job_course
FOREIGN KEY (course_id) REFERENCES courses(id);

-- Create index for better query performance
CREATE INDEX idx_quiz_preview_jobs_course_id ON quiz_preview_jobs(course_id);
