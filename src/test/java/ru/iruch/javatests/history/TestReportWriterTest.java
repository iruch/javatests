package ru.iruch.javatests.history;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.iruch.javatests.course.ScopeType;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestReportWriterTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TestReportWriter writer = new TestReportWriter(mapper);

    @Test
    void writesPromptAndAnswersForBothVariants() throws Exception {
        var attempt = new TestAttempt(AttemptStatus.SUBMITTED, ScopeType.TOPIC, 8L, "Java Core", "Java Core / Основы языка",
                "Основы языка: Final", "Europe/Moscow", Instant.parse("2026-11-19T22:30:00Z"),
                Instant.parse("2026-11-19T22:40:00Z"), 120, 2, 1, 1, 95, null);
        ReflectionTestUtils.setField(attempt, "id", 42L);
        attempt.addAnswer(new TestAttemptAnswer(1, 5L, "Основы языка", "Final", "Что означает final для класса?",
                "Класс нельзя наследовать.", "Нельзя наследовать", true, 60));
        attempt.addAnswer(new TestAttemptAnswer(2, 6L, "Основы языка", "Final", "Можно ли менять final-поле?",
                "Нет, после инициализации.", "", false, 35));

        List<TestAttemptFile> files = writer.write(attempt);

        assertThat(files).extracting(TestAttemptFile::getFileName).containsExactly(
                "JavaTestAgent_20.11.2026_01-40-00_full.md", "JavaTestResult_20.11.2026_01-40-00_full.json",
                "JavaTestAgent_20.11.2026_01-40-00_short.md", "JavaTestResult_20.11.2026_01-40-00_short.json");

        String fullPrompt = files.get(0).getContent();
        assertThat(fullPrompt)
                .contains("Режим отчёта: полный")
                .contains("Дата теста: 20.11.2026 01:40:00")
                .contains("Раздел: Java Core / Основы языка")
                .contains("Вопросов: 2, с ответом: 1, без ответа: 1, ответ подсмотрен: 1")
                .contains("Время на тест: 1 мин 35 с")
                .contains("`JavaTestResult_20.11.2026_01-40-00_full.json`")
                .doesNotContain("{{");
        assertThat(files.get(2).getContent()).contains("Режим отчёта: упрощённый").doesNotContain("{{");

        JsonNode json = mapper.readTree(files.get(3).getContent());
        assertThat(json.at("/test/id").asLong()).isEqualTo(42L);
        assertThat(json.at("/test/reportMode").asText()).isEqualTo("short");
        assertThat(json.at("/test/startedAt").asText()).isEqualTo("2026-11-20T01:30+03:00");
        assertThat(json.at("/summary/unanswered").asInt()).isEqualTo(1);
        assertThat(json.at("/answers/0/answerViewed").asBoolean()).isTrue();
        assertThat(json.at("/answers/0/referenceAnswer").asText()).isEqualTo("Класс нельзя наследовать.");
        assertThat(json.at("/answers/1/userAnswer").asText()).isEmpty();
    }
}
