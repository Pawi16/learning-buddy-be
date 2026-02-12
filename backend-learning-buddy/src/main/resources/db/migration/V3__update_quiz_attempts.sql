-- V2__update_quiz_attempts.sql

-- 1. Add 'status' column to track if quiz is IN_PROGRESS or COMPLETED
ALTER TABLE quiz_attempts 
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS';

-- 2. Make 'quiz_score' NULLABLE
-- (Because when a user starts a quiz, they have no score yet)
ALTER TABLE quiz_attempts 
ALTER COLUMN quiz_score DROP NOT NULL;