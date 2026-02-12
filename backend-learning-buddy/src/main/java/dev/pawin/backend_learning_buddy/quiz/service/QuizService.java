package dev.pawin.backend_learning_buddy.quiz.service;

import dev.pawin.backend_learning_buddy.auth.entity.User;
import dev.pawin.backend_learning_buddy.auth.repository.UserRepository;
import dev.pawin.backend_learning_buddy.course.entity.Course;
import dev.pawin.backend_learning_buddy.course.entity.Topic;
import dev.pawin.backend_learning_buddy.course.repository.CourseRepository;
import dev.pawin.backend_learning_buddy.course.repository.TopicRepository;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.CreateQuizResponse;
import dev.pawin.backend_learning_buddy.common.enumeration.AttemptStatus;
import dev.pawin.backend_learning_buddy.common.enumeration.SolutionVisibility;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizResultResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.SubmitQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.entity.AnswerHistory;
import dev.pawin.backend_learning_buddy.quiz.entity.Choice;
import dev.pawin.backend_learning_buddy.quiz.entity.Question;
import dev.pawin.backend_learning_buddy.quiz.entity.Quiz;
import dev.pawin.backend_learning_buddy.quiz.entity.QuizAttempt;
import dev.pawin.backend_learning_buddy.quiz.mapper.QuizMapper;
import dev.pawin.backend_learning_buddy.quiz.mapper.QuizResultMapper;
import dev.pawin.backend_learning_buddy.quiz.mapper.QuizResultMapperHelper;
import dev.pawin.backend_learning_buddy.quiz.repository.AnswerHistoryRepository;
import dev.pawin.backend_learning_buddy.quiz.repository.ChoiceRepository;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizAttemptRepository;
import dev.pawin.backend_learning_buddy.quiz.repository.QuizRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

        private final QuizRepository quizRepository;
        private final CourseRepository courseRepository;
        private final TopicRepository topicRepository;
        private final UserRepository userRepository;
        private final QuizMapper quizMapper;
        private final QuizResultMapper quizResultMapper;
        private final QuizResultMapperHelper quizResultMapperHelper;
        private final QuizAttemptRepository quizAttemptRepository;
        private final AnswerHistoryRepository answerHistoryRepository;
        private final ChoiceRepository choiceRepository;

        @Transactional
        public CreateQuizResponse createQuiz(Long courseId, CreateQuizRequest request, String username) {
                // Fetch and validate course
                Course course = courseRepository.findById(courseId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Course not found with id: " + courseId));

                // Check user permission (must be course creator)
                if (!course.getCreator().getUsername().equals(username)) {
                        throw new AccessDeniedException(
                                        "You do not have permission to create quizzes for this course.");
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
                                                "Topic with id " + questionDto.getTopicId()
                                                                + " does not belong to course with id " + courseId);
                        }

                        // Build Question entity
                        Question question = Question.builder()
                                        .quiz(quiz)
                                        .topic(topic)
                                        .questionText(questionDto.getQuestionText())
                                        .questionType(questionDto.getQuestionType())
                                        .difficultyLevel(questionDto.getDifficulty())
                                        .explanation(questionDto.getExplanation() != null ? questionDto.getExplanation()
                                                        : "")
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
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Course not found with id: " + courseId));

                // Get current user
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // Check if user is course owner
                boolean isOwner = course.getCreator().getId().equals(currentUser.getId());

                // Fetch quiz summaries (owner sees all, others see only published)
                return quizRepository.findQuizSummariesByCourseId(courseId, isOwner);
        }

        @Transactional
        public QuizExamDetailResponse getQuizForAttempt(Long quizId, String username) {
                // Fetch quiz with questions and choices
                Quiz quiz = quizRepository.findQuizByIdWithQuestions(quizId)
                                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with id: " + quizId));

                // Get current user
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // Access control: course owner can access unpublished quizzes, others cannot
                boolean isCourseOwner = quiz.getCourse().getCreator().getId().equals(currentUser.getId());
                if (!quiz.getIsPublished() && !isCourseOwner) {
                        throw new EntityNotFoundException("Quiz not found with id: " + quizId);
                }

                // Create QuizAttempt record
                QuizAttempt quizAttempt = QuizAttempt.builder()
                                .user(currentUser)
                                .quiz(quiz)
                                .startTime(LocalDateTime.now())
                                .status(AttemptStatus.IN_PROGRESS)
                                .build();
                quizAttemptRepository.save(quizAttempt);

                // Map to response DTO (security fields automatically excluded)
                return quizMapper.toQuizExamDetailResponse(quiz);
        }

        @Transactional
        public QuizResultResponse submitQuizAttempt(Long quizId, SubmitQuizRequest request, String username) {
                // 1. Fetch User
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // 2. Fetch Quiz with Questions
                Quiz quiz = quizRepository.findQuizByIdWithQuestions(quizId)
                                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with id: " + quizId));

                // 3. Fetch user's IN_PROGRESS QuizAttempt
                QuizAttempt quizAttempt = quizAttemptRepository
                                .findByUserIdAndQuizIdAndStatus(currentUser.getId(), quizId, AttemptStatus.IN_PROGRESS)
                                .orElseThrow(() -> new IllegalArgumentException("No in-progress quiz attempt found"));

                // 4. Validate Permission (Ownership check)
                if (!quizAttempt.getUser().getId().equals(currentUser.getId())) {
                        throw new AccessDeniedException("You do not have permission to submit this quiz attempt");
                }

                // 5. Fetch Choices Separately (Batch Query)
                List<Long> questionIds = quiz.getQuestions().stream()
                                .map(Question::getId)
                                .collect(Collectors.toList());

                List<Choice> allChoices = choiceRepository.findChoicesByQuestionIds(questionIds);

                // -------------------------------------------------------------
                // OPTIMIZATION: Build Maps for O(1) Lookup
                // -------------------------------------------------------------

                // Map 1: Question ID -> Question Entity
                Map<Long, Question> questionMap = quiz.getQuestions().stream()
                                .collect(Collectors.toMap(Question::getId, q -> q));

                // Map 2: Question ID -> (Choice ID -> Choice Entity)
                // We build this from 'allChoices' list directly.
                // We DO NOT touch question.getChoices() to avoid the Hibernate bug.
                Map<Long, Map<Long, Choice>> validChoicesMap = allChoices.stream()
                                .collect(Collectors.groupingBy(
                                                c -> c.getQuestion().getId(),
                                                Collectors.toMap(Choice::getId, c -> c)));

                // -------------------------------------------------------------
                // SCORING LOOP
                // -------------------------------------------------------------
                int score = 0;
                List<AnswerHistory> answerHistories = new ArrayList<>();

                for (SubmitQuizRequest.AnswerDto answerDto : request.getAnswers()) {
                        Long qId = answerDto.getQuestionId();
                        Long cId = answerDto.getChoiceId();

                        // 1. Validate Question
                        Question question = questionMap.get(qId);
                        if (question == null) {
                                throw new IllegalArgumentException("Invalid question ID for this quiz: " + qId);
                        }

                        // 2. Validate Choice (Look up in our separate Map)
                        Map<Long, Choice> questionChoices = validChoicesMap.get(qId);
                        if (questionChoices == null) {
                                throw new IllegalArgumentException("No choices found for question: " + qId);
                        }

                        Choice selectedChoice = questionChoices.get(cId);
                        if (selectedChoice == null) {
                                throw new IllegalArgumentException(
                                                "Choice ID " + cId + " does not belong to Question ID " + qId);
                        }

                        // 3. Score
                        if (Boolean.TRUE.equals(selectedChoice.getIsCorrect())) {
                                score++;
                        }

                        // 4. Record History
                        AnswerHistory answerHistory = AnswerHistory.builder()
                                        .quizAttempt(quizAttempt)
                                        .question(question)
                                        .selectedChoice(selectedChoice)
                                        .build();
                        answerHistories.add(answerHistory);
                }

                // 6. Save & Update
                answerHistoryRepository.saveAll(answerHistories);

                LocalDateTime endTime = LocalDateTime.now();
                quizAttempt.setEndTime(endTime);
                quizAttempt.setQuizScore(score);
                quizAttempt.setStatus(AttemptStatus.COMPLETED);
                quizAttemptRepository.save(quizAttempt);

                // 7. Calculate Duration & Feedback
                long durationSeconds = 0;
                if (quizAttempt.getStartTime() != null) {
                        durationSeconds = Duration.between(quizAttempt.getStartTime(), endTime).getSeconds();
                }

                boolean includeSolution = quiz.getSolutionVisibility() == SolutionVisibility.ALWAYS;

                return quizResultMapper.toQuizResultResponse(
                                quizAttempt,
                                quiz.getQuestions().size(),
                                durationSeconds,
                                quizResultMapperHelper.buildFeedback(answerHistories, quiz.getQuestions(),
                                                includeSolution));
        }
}
