-- 1. Users Table (Role is now VARCHAR)
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       role VARCHAR(50) NOT NULL DEFAULT 'USER', -- Changed from INTEGER DEFAULT 1
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
                         id BIGSERIAL PRIMARY KEY,
                         creator_id BIGINT NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         is_published BOOLEAN NOT NULL DEFAULT false,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE enrollments (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             course_id BIGINT NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                             updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE topics (
                        id BIGSERIAL PRIMARY KEY,
                        course_id BIGINT NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        raw_text TEXT,
                        summary_note TEXT,
                        order_index INTEGER NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE topic_progress (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                topic_id BIGINT NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE decks (
                       id BIGSERIAL PRIMARY KEY,
                       course_id BIGINT NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       is_published BOOLEAN NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE flashcards (
                            id BIGSERIAL PRIMARY KEY,
                            deck_id BIGINT NOT NULL,
                            topic_id BIGINT NOT NULL,
                            front_text TEXT NOT NULL,
                            back_text TEXT NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Quizzes Table (Solution Visibility is now VARCHAR)
CREATE TABLE quizzes (
                         id BIGSERIAL PRIMARY KEY,
                         course_id BIGINT NOT NULL,
                         title VARCHAR(255) NOT NULL,
                         solution_visibility VARCHAR(50) NOT NULL DEFAULT 'ALWAYS', -- Changed from INTEGER
                         is_published BOOLEAN NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Questions Table (Type and Difficulty are now VARCHAR)
CREATE TABLE questions (
                           id BIGSERIAL PRIMARY KEY,
                           quiz_id BIGINT NOT NULL,
                           topic_id BIGINT NOT NULL,
                           question_text TEXT NOT NULL,
                           question_type VARCHAR(50) NOT NULL, -- Changed from INTEGER
                           explanation TEXT NOT NULL,
                           difficulty_level VARCHAR(50) NOT NULL, -- Changed from INTEGER
                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE choices (
                         id BIGSERIAL PRIMARY KEY,
                         question_id BIGINT NOT NULL,
                         choice_text VARCHAR(255) NOT NULL,
                         is_correct BOOLEAN NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE quiz_attempts (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               quiz_id BIGINT NOT NULL,
                               quiz_score INTEGER NOT NULL,
                               start_time TIMESTAMP,
                               end_time TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE answer_history (
                                id BIGSERIAL PRIMARY KEY,
                                quiz_attempt_id BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                selected_choice_id BIGINT NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Foreign Keys (No changes needed here)
ALTER TABLE enrollments ADD FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE courses ADD FOREIGN KEY (creator_id) REFERENCES users (id);
ALTER TABLE topic_progress ADD FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE quiz_attempts ADD FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE enrollments ADD FOREIGN KEY (course_id) REFERENCES courses (id);
ALTER TABLE topics ADD FOREIGN KEY (course_id) REFERENCES courses (id);
ALTER TABLE decks ADD FOREIGN KEY (course_id) REFERENCES courses (id);
ALTER TABLE quizzes ADD FOREIGN KEY (course_id) REFERENCES courses (id);
ALTER TABLE topic_progress ADD FOREIGN KEY (topic_id) REFERENCES topics (id);
ALTER TABLE flashcards ADD FOREIGN KEY (topic_id) REFERENCES topics (id);
ALTER TABLE questions ADD FOREIGN KEY (topic_id) REFERENCES topics (id);
ALTER TABLE flashcards ADD FOREIGN KEY (deck_id) REFERENCES decks (id);
ALTER TABLE questions ADD FOREIGN KEY (quiz_id) REFERENCES quizzes (id);
ALTER TABLE choices ADD FOREIGN KEY (question_id) REFERENCES questions (id);
ALTER TABLE quiz_attempts ADD FOREIGN KEY (quiz_id) REFERENCES quizzes (id);
ALTER TABLE answer_history ADD FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempts (id);
ALTER TABLE answer_history ADD FOREIGN KEY (question_id) REFERENCES questions (id);
ALTER TABLE answer_history ADD FOREIGN KEY (selected_choice_id) REFERENCES choices (id);