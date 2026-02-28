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
import dev.pawin.backend_learning_buddy.quiz.dto.QuizDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizExamDetailResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizResultResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.QuizSummaryResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.SubmitQuizRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizContentRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizContentResponse;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizMetadataRequest;
import dev.pawin.backend_learning_buddy.quiz.dto.UpdateQuizMetadataResponse;
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
import java.util.*;
import java.util.function.Function;
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
                                                .explanation(choiceDto.getExplanation())
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

                // Check for existing IN_PROGRESS attempt
                Optional<QuizAttempt> existingAttempt = quizAttemptRepository
                                .findByUserIdAndQuizIdAndStatus(currentUser.getId(), quizId, AttemptStatus.IN_PROGRESS);

                QuizAttempt quizAttempt;
                if (existingAttempt.isPresent()) {
                        // Reuse existing IN_PROGRESS attempt
                        quizAttempt = existingAttempt.get();
                } else {
                        // Create new QuizAttempt record
                        quizAttempt = QuizAttempt.builder()
                                        .user(currentUser)
                                        .quiz(quiz)
                                        .startTime(LocalDateTime.now())
                                        .status(AttemptStatus.IN_PROGRESS)
                                        .build();
                        quizAttemptRepository.save(quizAttempt);
                }

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

        @Transactional(readOnly = true)
        public QuizDetailResponse getQuizDetail(Long quizId, String username) {
                // 1. Fetch User
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // 2. Fetch Quiz with Questions (using existing query)
                Quiz quiz = quizRepository.findQuizByIdWithQuestions(quizId)
                                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

                // 3. Check if user is course owner
                boolean isOwner = quiz.getCourse().getCreator().getId().equals(currentUser.getId());

                // 4. Access control: only course owners can access quiz details
                if (!isOwner) {
                        throw new AccessDeniedException("You do not have permission to view this quiz");
                }

                // 5. Fetch Choices Separately (Batch Query) - avoiding Hibernate two-layer issue
                List<Long> questionIds = quiz.getQuestions().stream()
                                .map(Question::getId)
                                .collect(Collectors.toList());

                List<Choice> allChoices = choiceRepository.findChoicesByQuestionIds(questionIds);

                // 6. Build Map: Question ID -> List of Choices
                Map<Long, List<Choice>> choicesByQuestion = allChoices.stream()
                                .collect(Collectors.groupingBy(c -> c.getQuestion().getId()));

                // 7. Map to Response DTO (manually combining questions + choices)
                List<QuizDetailResponse.QuestionDetailDto> questionDtos = quiz.getQuestions().stream()
                                .map(question -> {
                                        List<Choice> questionChoices = choicesByQuestion.getOrDefault(question.getId(), List.of());
                                        return QuizDetailResponse.QuestionDetailDto.builder()
                                                        .id(question.getId())
                                                        .topicId(question.getTopic().getId())
                                                        .questionText(question.getQuestionText())
                                                        .questionType(question.getQuestionType().name())
                                                        .difficulty(question.getDifficultyLevel() != null ? question.getDifficultyLevel().name() : null)
                                                        .explanation(question.getExplanation())
                                                        .choices(questionChoices.stream()
                                                                        .map(choice -> QuizDetailResponse.ChoiceDetailDto.builder()
                                                                                        .id(choice.getId())
                                                                                        .choiceText(choice.getChoiceText())
                                                                                        .isCorrect(choice.getIsCorrect())
                                                                                        .explanation(choice.getExplanation())
                                                                                        .build())
                                                                        .collect(Collectors.toList()))
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return QuizDetailResponse.builder()
                                .quizId(quiz.getId())
                                .courseId(quiz.getCourse().getId())
                                .title(quiz.getTitle())
                                .solutionVisibility(quiz.getSolutionVisibility())
                                .isPublished(quiz.getIsPublished())
                                .createdAt(quiz.getCreatedAt())
                                .updatedAt(quiz.getUpdatedAt())
                                .questions(questionDtos)
                                .build();
        }

        @Transactional
        public UpdateQuizMetadataResponse updateQuizMetadata(Long quizId, String username, UpdateQuizMetadataRequest request) {
                // 1. Fetch Quiz
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

                // 2. Fetch User
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // 3. Validate Ownership (course creator only)
                if (!quiz.getCourse().getCreator().getId().equals(currentUser.getId())) {
                        throw new AccessDeniedException("You do not have permission to edit this quiz");
                }

                // 4. Partial Update: Title (required but nullable check)
                if (request.getTitle() != null) {
                        if (request.getTitle().isBlank()) {
                                throw new IllegalArgumentException("Title is required");
                        }
                        quiz.setTitle(request.getTitle());
                }

                // 5. Partial Update: Solution Visibility (optional)
                if (request.getSolutionVisibility() != null) {
                        quiz.setSolutionVisibility(request.getSolutionVisibility());
                }

                // 6. Partial Update: Is Published (optional)
                if (request.getIsPublished() != null) {
                        quiz.setIsPublished(request.getIsPublished());
                }

                // 7. Save (updated_at is handled automatically by @PreUpdate in BaseEntity)
                Quiz savedQuiz = quizRepository.save(quiz);

                // 8. Build Response with full metadata
                return UpdateQuizMetadataResponse.builder()
                                .quizMetadata(quizMapper.toQuizMetadataResponse(savedQuiz))
                                .message("Quiz metadata updated successfully")
                                .build();
        }

        @Transactional
        public UpdateQuizContentResponse updateQuizContent(Long quizId, String username, UpdateQuizContentRequest request) {
                // 1. Fetch Quiz with Questions (use existing query)
                Quiz quiz = quizRepository.findQuizByIdWithQuestions(quizId)
                                .orElseThrow(() -> new EntityNotFoundException("Quiz not found"));

                // 2. Fetch Current User
                User currentUser = userRepository.findByUsername(username)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // 3. Validate Ownership (course creator only)
                if (!quiz.getCourse().getCreator().getId().equals(currentUser.getId())) {
                        throw new AccessDeniedException("You do not have permission to update this quiz content");
                }

                // 4. Build map of incoming questions with IDs (for O(1) lookup)
                Map<Long, UpdateQuizContentRequest.QuestionDetailDto> incomingQuestionMap = request.getQuestions().stream()
                                .filter(q -> q.getId() != null)
                                .collect(Collectors.toMap(UpdateQuizContentRequest.QuestionDetailDto::getId, Function.identity()));

                // 5. Process Existing Questions: Update or Delete
                Iterator<Question> questionIterator = quiz.getQuestions().iterator();

                while (questionIterator.hasNext()) {
                        Question existingQuestion = questionIterator.next();

                        if (incomingQuestionMap.containsKey(existingQuestion.getId())) {
                                // UPDATE: Update question fields
                                UpdateQuizContentRequest.QuestionDetailDto incomingQuestion = incomingQuestionMap.get(existingQuestion.getId());
                                updateQuestionFromDto(existingQuestion, incomingQuestion);
                                incomingQuestionMap.remove(existingQuestion.getId()); // Remove from map
                        } else {
                                // DELETE: Question not in incoming payload → remove (cascade deletes choices)
                                questionIterator.remove();
                        }
                }

                // 6. Insert New Questions (those with null IDs)
                List<UpdateQuizContentRequest.QuestionDetailDto> newQuestions = request.getQuestions().stream()
                                .filter(q -> q.getId() == null)
                                .toList();

                for (UpdateQuizContentRequest.QuestionDetailDto questionDto : newQuestions) {
                        // Fetch and validate topic
                        Topic topic = topicRepository.findById(questionDto.getTopicId())
                                        .orElseThrow(() -> new EntityNotFoundException("Topic not found with id: " + questionDto.getTopicId()));

                        // Verify topic belongs to the quiz's course
                        if (!topic.getCourse().getId().equals(quiz.getCourse().getId())) {
                                throw new IllegalArgumentException(
                                                "Topic with id " + questionDto.getTopicId()
                                                        + " does not belong to course with id " + quiz.getCourse().getId());
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

                        // Build and add Choices (bidirectional)
                        for (UpdateQuizContentRequest.ChoiceDetailDto choiceDto : questionDto.getChoices()) {
                                Choice choice = Choice.builder()
                                                .question(question)
                                                .choiceText(choiceDto.getChoiceText())
                                                .isCorrect(choiceDto.getIsCorrect())
                                                .explanation(choiceDto.getExplanation())
                                                .build();
                                question.getChoices().add(choice);
                        }

                        // Add question to quiz (establishes bidirectional relationship)
                        quiz.getQuestions().add(question);
                }

                // 7. Validation: If incomingQuestionMap is not empty, means user sent question IDs belonging to other quiz
                if (!incomingQuestionMap.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Invalid Question IDs provided (do not belong to this quiz): " + incomingQuestionMap.keySet());
                }

                // 8. Save (cascade handles all CRUD operations)
                quizRepository.save(quiz);

                return UpdateQuizContentResponse.builder()
                                .message("Quiz content saved successfully.")
                                .build();
        }

        private void updateQuestionFromDto(Question existingQuestion, UpdateQuizContentRequest.QuestionDetailDto dto) {
                // Update basic fields
                existingQuestion.setQuestionText(dto.getQuestionText());
                existingQuestion.setQuestionType(dto.getQuestionType());
                existingQuestion.setDifficultyLevel(dto.getDifficulty());
                existingQuestion.setExplanation(dto.getExplanation() != null ? dto.getExplanation() : "");

                // Handle Topic update (validate belongs to same course)
                if (!existingQuestion.getTopic().getId().equals(dto.getTopicId())) {
                        Topic newTopic = topicRepository.findById(dto.getTopicId())
                                        .orElseThrow(() -> new EntityNotFoundException("Topic not found with id: " + dto.getTopicId()));

                        if (!newTopic.getCourse().getId().equals(existingQuestion.getQuiz().getCourse().getId())) {
                                throw new IllegalArgumentException(
                                                "Topic with id " + dto.getTopicId()
                                                        + " does not belong to course with id " + existingQuestion.getQuiz().getCourse().getId());
                        }
                        existingQuestion.setTopic(newTopic);
                }

                // Build map of incoming choices with IDs
                Map<Long, UpdateQuizContentRequest.ChoiceDetailDto> incomingChoiceMap = dto.getChoices().stream()
                                .filter(c -> c.getId() != null)
                                .collect(Collectors.toMap(UpdateQuizContentRequest.ChoiceDetailDto::getId, Function.identity()));

                // Process Existing Choices: Update or Delete
                Iterator<Choice> choiceIterator = existingQuestion.getChoices().iterator();

                while (choiceIterator.hasNext()) {
                        Choice existingChoice = choiceIterator.next();

                        if (incomingChoiceMap.containsKey(existingChoice.getId())) {
                                // UPDATE
                                UpdateQuizContentRequest.ChoiceDetailDto incomingChoice = incomingChoiceMap.get(existingChoice.getId());
                                existingChoice.setChoiceText(incomingChoice.getChoiceText());
                                existingChoice.setIsCorrect(incomingChoice.getIsCorrect());
                                existingChoice.setExplanation(incomingChoice.getExplanation());
                                incomingChoiceMap.remove(existingChoice.getId());
                        } else {
                                // DELETE (cascade not needed, Choice has no children)
                                choiceIterator.remove();
                        }
                }

                // Insert New Choices (those with null IDs)
                List<UpdateQuizContentRequest.ChoiceDetailDto> newChoices = dto.getChoices().stream()
                                .filter(c -> c.getId() == null)
                                .toList();

                for (UpdateQuizContentRequest.ChoiceDetailDto choiceDto : newChoices) {
                        Choice newChoice = Choice.builder()
                                        .question(existingQuestion)
                                        .choiceText(choiceDto.getChoiceText())
                                        .isCorrect(choiceDto.getIsCorrect())
                                        .build();
                        existingQuestion.getChoices().add(newChoice);
                }

                // Validation: Check for stray choice IDs
                if (!incomingChoiceMap.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Invalid Choice IDs provided (do not belong to this question): " + incomingChoiceMap.keySet());
                }
        }
}
