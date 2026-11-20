package ru.iruch.javatests.history;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.course.Question;
import ru.iruch.javatests.course.QuestionRepository;
import ru.iruch.javatests.course.ScopeType;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TestHistoryService {
    public static final int SECONDS_PER_QUESTION = 120;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final CourseService courseService;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestReportWriter reportWriter;

    public TestHistoryService(CourseService courseService, QuestionRepository questionRepository,
                              TestAttemptRepository attemptRepository, TestReportWriter reportWriter) {
        this.courseService = courseService;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.reportWriter = reportWriter;
    }

    @Transactional
    public RecordedAttempt record(AttemptRequest request) {
        var type = ScopeType.fromSegment(request.scope());
        var scope = courseService.getCatalog().scope(type, request.scopeId());
        Map<Long, Question> questions = questionRepository.findAllWithLessonByIdIn(
                        request.answers().stream().map(AnswerRequest::questionId).toList()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (questions.size() != request.answers().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or repeated question ids");
        }
        int count = request.answers().size();
        Integer failedNumber = request.status() == AttemptStatus.FAILED ? request.failedQuestionNumber() : null;
        if (request.status() == AttemptStatus.FAILED && (failedNumber == null || failedNumber < 1 || failedNumber > count)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failedQuestionNumber is required for a failed test");
        }

        List<Question> ordered = request.answers().stream().map(a -> questions.get(a.questionId())).toList();
        var attempt = new TestAttempt(request.status(), type, request.scopeId(), scope.courseTitle(), scope.path(),
                coveredSections(ordered), zone(request.timeZone()).getId(), request.startedAt(), request.finishedAt(),
                SECONDS_PER_QUESTION, count,
                (int) request.answers().stream().filter(a -> !a.text().isEmpty()).count(),
                (int) request.answers().stream().filter(AnswerRequest::answerViewed).count(),
                request.answers().stream().mapToInt(a -> Math.max(0, a.timeSpentSec())).sum(),
                failedNumber);
        if (request.status() == AttemptStatus.SUBMITTED) {
            for (int i = 0; i < count; i++) {
                AnswerRequest answer = request.answers().get(i);
                Question question = ordered.get(i);
                attempt.addAnswer(new TestAttemptAnswer(i + 1, question.getId(), question.getLesson().getTopic().getTitle(),
                        question.getLesson().getTitle(), question.getQuestionText(), question.getAnswerText(),
                        answer.text(), answer.answerViewed(), Math.max(0, answer.timeSpentSec())));
            }
        }
        attempt = attemptRepository.save(attempt);
        if (request.status() == AttemptStatus.SUBMITTED) {
            reportWriter.write(attempt).forEach(attempt::addFile);
        }
        return new RecordedAttempt(attempt.getId(), attempt.getStatus(), attempt.getQuestionCount(),
                attempt.getAnsweredCount(), attempt.getSkippedCount(), attempt.getViewedCount(),
                Durations.format(attempt.getTotalTimeSec()));
    }

    @Transactional(readOnly = true)
    public List<AttemptView> getHistory() {
        List<TestAttempt> attempts = attemptRepository.findAllByOrderByFinishedAtDescIdDesc();
        Map<Long, List<TestAttemptAnswer>> answers = attempts.isEmpty() ? Map.of()
                : attemptRepository.findAnswersByAttemptIds(attempts.stream().map(TestAttempt::getId).toList()).stream()
                .collect(Collectors.groupingBy(a -> a.getAttempt().getId()));
        return attempts.stream().map(a -> view(a, answers.getOrDefault(a.getId(), List.of()))).toList();
    }

    /** Removes the attempt with its answers and report files; 404 if it does not exist. */
    @Transactional
    public void delete(long attemptId) {
        if (attemptRepository.deleteAttemptById(attemptId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found");
        }
    }

    @Transactional(readOnly = true)
    public TestAttemptFile getFile(long attemptId, ReportVariant variant, ReportVariant.Format format) {
        return attemptRepository.findFile(attemptId, variant, format)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    private static AttemptView view(TestAttempt attempt, List<TestAttemptAnswer> answers) {
        ZoneId zone = ZoneId.of(attempt.getTimeZone());
        var finished = attempt.getFinishedAt().atZone(zone);
        boolean failed = attempt.getStatus() == AttemptStatus.FAILED;
        return new AttemptView(attempt.getId(), failed, failed ? "Не сдан" : "Сдан на проверку",
                DATE.format(finished), TIME.format(finished), attempt.getCourseTitle(), attempt.getScopePath(),
                attempt.getScopeType().url(attempt.getScopeId()), attempt.getCoveredSections(),
                attempt.getQuestionCount(), attempt.getAnsweredCount(), attempt.getSkippedCount(),
                attempt.getViewedCount(), Durations.format(attempt.getTotalTimeSec()),
                DATE_TIME.format(attempt.getStartedAt().atZone(zone)), DATE_TIME.format(finished),
                attempt.getTimeZone(), attempt.getSecondsPerQuestion() / 60, attempt.getFailedQuestionNumber(),
                answers.stream().map(a -> new AnswerView(a.getNumber(), a.getTopicTitle(), a.getLessonTitle(),
                        a.getQuestionText(), a.getUserAnswer(), a.isAnswerViewed(), Durations.format(a.getTimeSpentSec()),
                        a.getReferenceAnswer())).toList());
    }

    /** "Тема: урок, урок; Тема: урок" in the order the questions were asked. */
    static String coveredSections(List<Question> questions) {
        Map<String, Set<String>> lessonsByTopic = new LinkedHashMap<>();
        for (Question question : questions) {
            lessonsByTopic.computeIfAbsent(question.getLesson().getTopic().getTitle(), t -> new LinkedHashSet<>())
                    .add(question.getLesson().getTitle());
        }
        return lessonsByTopic.entrySet().stream()
                .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
                .collect(Collectors.joining("; "));
    }

    private static ZoneId zone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return ZoneId.of("UTC");
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }

    public record AttemptRequest(@NotBlank String scope, @NotNull Long scopeId, @NotNull AttemptStatus status,
                                 @NotNull Instant startedAt, @NotNull Instant finishedAt, String timeZone,
                                 Integer failedQuestionNumber, @NotEmpty List<@Valid AnswerRequest> answers) {}

    public record AnswerRequest(@NotNull Long questionId, String userAnswer, boolean answerViewed, int timeSpentSec) {
        String text() { return userAnswer == null ? "" : userAnswer.strip(); }
    }

    public record RecordedAttempt(Long id, AttemptStatus status, int questionCount, int answered, int skipped,
                                  int viewed, String totalTime) {}

    public record AttemptView(Long id, boolean failed, String statusLabel, String date, String time,
                              String courseTitle, String scopePath, String scopeUrl, String coveredSections,
                              int questionCount, int answered, int skipped, int viewed, String totalTime,
                              String startedAt, String finishedAt, String timeZone, int minutesPerQuestion,
                              Integer failedQuestionNumber, List<AnswerView> answers) {}

    public record AnswerView(int number, String topic, String lesson, String question, String userAnswer,
                             boolean viewed, String timeSpent, String reference) {}
}
