package ru.iruch.javatests.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.iruch.javatests.course.Catalog;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.history.AttemptStatus;
import ru.iruch.javatests.history.ReportVariant;
import ru.iruch.javatests.history.TestAttemptFile;
import ru.iruch.javatests.history.TestHistoryService;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HistoryController.class, HelpController.class})
class HistoryPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean CourseService courseService;
    @MockitoBean TestHistoryService historyService;

    @BeforeEach
    void catalog() {
        given(courseService.getCatalog()).willReturn(new Catalog(List.of()));
    }

    @Test
    void historyListsAttemptsWithExpandableDetailsAndDownloads() throws Exception {
        given(historyService.getHistory()).willReturn(List.of(
                new TestHistoryService.AttemptView(7L, false, "Сдан на проверку", "20.11.2026", "10:15", "Java Core",
                        "Java Core / Основы языка", "/topics/8", "Основы языка: Final", 2, 1, 1, 1, "1 мин 35 с",
                        "20.11.2026 10:13:40", "20.11.2026 10:15:15", "Europe/Moscow", 2, null,
                        List.of(new TestHistoryService.AnswerView(1, "Основы языка", "Final", "Что означает final для класса?",
                                "Нельзя наследовать", true, "1 мин 0 с", "Класс нельзя наследовать."))),
                new TestHistoryService.AttemptView(6L, true, "Не сдан", "19.11.2026", "21:00", "Java Core",
                        "Java Core", "/courses/1", "Основы языка: ООП", 10, 3, 7, 0, "4 мин 1 с",
                        "19.11.2026 20:56:00", "19.11.2026 21:00:01", "Europe/Moscow", 2, 4, List.of())));

        mvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"))
                .andExpect(content().string(containsString("Моя история тестирования")))
                .andExpect(content().string(containsString("data-history-toggle=\"attempt-7-details\"")))
                .andExpect(content().string(containsString("data-download-files=\"/history/7/download/full\"")))
                .andExpect(content().string(containsString("data-download-files=\"/history/7/download/short\"")))
                .andExpect(content().string(containsString("Скачать результат (упрощенный)")))
                .andExpect(content().string(containsString("Что означает final для класса?")))
                .andExpect(content().string(containsString("Время на вопрос 4 из 10 истекло")))
                .andExpect(content().string(containsString("action=\"/history/7/delete\"")))
                .andExpect(content().string(containsString("action=\"/history/6/delete\"")))
                .andExpect(content().string(containsString("data-confirm=\"Удалить тест от 20.11.2026 10:15")))
                .andExpect(content().string(containsString("aria-current=\"page\"")));
    }

    @Test
    void deleteRemovesTheAttemptAndReturnsToHistoryWithANotice() throws Exception {
        mvc.perform(post("/history/7/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/history"))
                .andExpect(flash().attribute("deletedNotice", containsString("удалён")));
        then(historyService).should().delete(7L);
    }

    @Test
    void deletingAMissingAttemptIsNotFound() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).given(historyService).delete(404L);
        mvc.perform(post("/history/404/delete")).andExpect(status().isNotFound());
    }

    @Test
    void downloadReturnsTheStoredFileAsAttachment() throws Exception {
        given(historyService.getFile(7L, ReportVariant.FULL, ReportVariant.Format.MD)).willReturn(
                new TestAttemptFile(ReportVariant.FULL, ReportVariant.Format.MD, "JavaTestAgent_20.11.2026_14-05-33_full.md", "# Промпт"));
        mvc.perform(get("/history/7/download/full/md"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("JavaTestAgent_20.11.2026_14-05-33_full.md")))
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string("# Промпт"));
        mvc.perform(get("/history/7/download/medium/md")).andExpect(status().isNotFound());
    }

    @Test
    void savingASubmittedTestReturnsDownloadLinksForBothVariants() throws Exception {
        given(historyService.record(any())).willReturn(
                new TestHistoryService.RecordedAttempt(9L, AttemptStatus.SUBMITTED, 2, 1, 1, 0, "40 с"));
        mvc.perform(post("/api/test-attempts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"scope":"topics","scopeId":8,"status":"SUBMITTED",
                         "startedAt":"2026-11-20T07:00:00Z","finishedAt":"2026-11-20T07:00:40Z","timeZone":"Europe/Moscow",
                         "answers":[{"questionId":1,"userAnswer":"да","answerViewed":false,"timeSpentSec":20},
                                    {"questionId":2,"userAnswer":"","answerViewed":false,"timeSpentSec":20}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attempt.id").value(9))
                .andExpect(jsonPath("$.historyUrl").value("/history#attempt-9"))
                .andExpect(jsonPath("$.downloads.full").value("/history/9/download/full"))
                .andExpect(jsonPath("$.downloads.short").value("/history/9/download/short"));
    }

    @Test
    void savingWithoutAnswersIsRejected() throws Exception {
        mvc.perform(post("/api/test-attempts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"scope":"topics","scopeId":8,"status":"SUBMITTED",
                         "startedAt":"2026-11-20T07:00:00Z","finishedAt":"2026-11-20T07:00:40Z","answers":[]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void helpRendersTheProjectReadme() throws Exception {
        mvc.perform(get("/help"))
                .andExpect(status().isOk())
                .andExpect(view().name("help"))
                .andExpect(content().string(containsString("<h1>Java-тесты для Study Hub</h1>")))
                .andExpect(content().string(containsString("Как прогнать результат через ИИ")))
                .andExpect(content().string(containsString("<table>")));
    }
}
