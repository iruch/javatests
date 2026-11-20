package ru.iruch.javatests.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the agent prompt (.md) and the answers file (.json) of a submitted attempt for every report variant. */
@Component
public class TestReportWriter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    /** Several tests a day are normal, so the time is part of the name; Windows forbids ":" in file names. */
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH-mm-ss");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final ObjectMapper objectMapper;
    private final Map<ReportVariant, String> templates = new EnumMap<>(ReportVariant.class);

    public TestReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (ReportVariant variant : ReportVariant.values()) {
            templates.put(variant, readTemplate(variant.template()));
        }
    }

    public List<TestAttemptFile> write(TestAttempt attempt) {
        List<TestAttemptFile> files = new ArrayList<>();
        for (ReportVariant variant : ReportVariant.values()) {
            String suffix = format(FILE_STAMP, attempt) + "_" + variant.slug();
            String jsonName = "JavaTestResult_" + suffix + ".json";
            String mdName = "JavaTestAgent_" + suffix + ".md";
            files.add(new TestAttemptFile(variant, ReportVariant.Format.MD, mdName, markdown(attempt, variant, jsonName)));
            files.add(new TestAttemptFile(variant, ReportVariant.Format.JSON, jsonName, json(attempt, variant)));
        }
        return files;
    }

    String markdown(TestAttempt attempt, ReportVariant variant, String resultFile) {
        Map<String, String> values = Map.of(
                "date", format(DATE_TIME, attempt),
                "course", attempt.getCourseTitle(),
                "scope", attempt.getScopePath(),
                "sections", attempt.getCoveredSections(),
                "questionCount", String.valueOf(attempt.getQuestionCount()),
                "answered", String.valueOf(attempt.getAnsweredCount()),
                "skipped", String.valueOf(attempt.getSkippedCount()),
                "viewed", String.valueOf(attempt.getViewedCount()),
                "totalTime", Durations.format(attempt.getTotalTimeSec()),
                "resultFile", resultFile);
        Matcher matcher = PLACEHOLDER.matcher(templates.get(variant));
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    String json(TestAttempt attempt, ReportVariant variant) {
        ZoneId zone = ZoneId.of(attempt.getTimeZone());
        var report = new Report(
                new TestInfo(attempt.getId(), variant.slug(), attempt.getCourseTitle(), attempt.getScopePath(),
                        attempt.getCoveredSections(), format(DATE, attempt),
                        attempt.getStartedAt().atZone(zone).toOffsetDateTime().toString(),
                        attempt.getFinishedAt().atZone(zone).toOffsetDateTime().toString(),
                        attempt.getTimeZone(), attempt.getQuestionCount(), attempt.getSecondsPerQuestion()),
                new Summary(attempt.getAnsweredCount(), attempt.getSkippedCount(), attempt.getViewedCount(),
                        attempt.getTotalTimeSec()),
                attempt.getAnswers().stream()
                        .map(a -> new Answer(a.getNumber(), a.getTopicTitle(), a.getLessonTitle(), a.getQuestionText(),
                                a.getUserAnswer(), a.isAnswerViewed(), a.getReferenceAnswer(), a.getTimeSpentSec()))
                        .toList());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize test report", e);
        }
    }

    private static String format(DateTimeFormatter formatter, TestAttempt attempt) {
        return formatter.format(attempt.getFinishedAt().atZone(ZoneId.of(attempt.getTimeZone())));
    }

    private static String readTemplate(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing prompt template " + path, e);
        }
    }

    record Report(TestInfo test, Summary summary, List<Answer> answers) {}

    record TestInfo(Long id, String reportMode, String course, String title, String sections, String date,
                    String startedAt, String finishedAt, String timeZone, int questionCount, int secondsPerQuestion) {}

    record Summary(int answered, int unanswered, int answerViewed, int totalTimeSec) {}

    record Answer(int number, String topic, String lesson, String question, String userAnswer, boolean answerViewed,
                  String referenceAnswer, int timeSpentSec) {}
}
