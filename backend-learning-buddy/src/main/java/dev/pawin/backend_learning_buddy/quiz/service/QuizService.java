package dev.pawin.backend_learning_buddy.quiz.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.entity.Choice;
import dev.pawin.backend_learning_buddy.quiz.entity.Question;
import dev.pawin.backend_learning_buddy.quiz.entity.Quiz;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateQuizResponse createQuiz(Long courseId, CreateQuizRequest request, String username) {
        // Fetch and validate course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // Check user permission (must be course creator)
        if (!course.getCreator().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to create quizzes for this course.");
        }

        // Validate title (defensive check, already validated by @NotBlank)
        if (request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Quiz title cannot be blank");
        }

        // Build Quiz entity
        Quiz quiz = Quiz.builder()
                .course(course)
                .title(request.getTitle())
                .solutionVisibility(request.getSolutionVisibility())
                .isPublished(request.getIsPublished())
                .build();

        // Build Question and Choice entities with bidirectional relationships
        for (CreateQuizRequest.QuestionDto questionDto : request.getQuestions()) {
            // Fetch and validate topic
            Topic topic = topicRepository.findById(questionDto.getTopicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Topic not found with id: " + questionDto.getTopicId()));

            // Verify topic belongs to the specified course
            if (!topic.getCourse().getId().equals(courseId)) {
                throw new IllegalArgumentException(
                        "Topic with id " + questionDto.getTopicId() + " does not belong to course with id " + courseId);
            }

            // Build Question entity
            Question question = Question.builder()
                    .quiz(quiz)
                    .topic(topic)
                    .questionText(questionDto.getQuestionText())
                    .questionType(questionDto.getQuestionType())
                    .difficultyLevel(questionDto.getDifficulty())
                    .explanation(questionDto.getExplanation() != null ? questionDto.getExplanation() : "")
                    .build();

            // Build Choice entities with bidirectional relationship
            for (CreateQuizRequest.ChoiceDto choiceDto : questionDto.getChoices()) {
                Choice choice = Choice.builder()
                        .question(question)
                        .choiceText(choiceDto.getChoiceText())
                        .isCorrect(choiceDto.getIsCorrect())
                        .build();
                question.getChoices().add(choice);
            }

            // Add question to quiz (establishes bidirectional relationship)
            quiz.getQuestions().add(question);
        }

        // Save quiz (cascade will save questions and choices)
        Quiz savedQuiz = quizRepository.save(quiz);

        // Build and return response
        return CreateQuizResponse.builder()
                .quizId(savedQuiz.getId())
                .message("Quiz created successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryResponse> getQuizzesByCourseId(Long courseId, String username) {
        // Validate course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        // Get current user
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if user is course owner
        boolean isOwner = course.getCreator().getId().equals(currentUser.getId());

        // Fetch quiz summaries (owner sees all, others see only published)
        return quizRepository.findQuizSummariesByCourseId(courseId, isOwner);
    }
}
