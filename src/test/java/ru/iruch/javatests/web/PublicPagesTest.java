package ru.iruch.javatests.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.iruch.javatests.course.Catalog;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.course.ScopeType;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HomeController.class, QuizApiController.class})
class PublicPagesTest {
    private static final CourseService.QuestionView FINAL_QUESTION = new CourseService.QuestionView(
            1L, "Основы языка", "Final", "Что означает final для класса?", "Класс нельзя наследовать (например, String).");

    @Autowired MockMvc mvc;
    @MockitoBean CourseService courseService;

    @BeforeEach
    void catalog() {
        given(courseService.getCatalog()).willReturn(new Catalog(List.of(
                new Catalog.CourseView(1L, "java-core", "Java Core", 25, List.of(
                        new Catalog.TopicView(11L, "Основы языка", 25, List.of(
                                new Catalog.LessonView(101L, "Final", 4),
                                new Catalog.LessonView(102L, "Static", 21))),
                        new Catalog.TopicView(12L, "Java Stream API", 0, List.of()))))));
    }

    @Test
    void homeIsPublicAndRendersTheCourseTree() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Java Tests")))
                .andExpect(content().string(containsString("Обучение")))
                .andExpect(content().string(not(containsString("Подготовка к собеседованию"))))
                .andExpect(content().string(containsString("/courses/1")))
                .andExpect(content().string(containsString("/topics/11")))
                .andExpect(content().string(containsString("/lessons/101")));
    }

    @Test
    void courseSectionShowsTotalQuestionsAndLinkedBreadcrumbs() throws Exception {
        given(courseService.getQuestions(ScopeType.COURSE, 1L)).willReturn(List.of(FINAL_QUESTION));
        mvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("section"))
                .andExpect(content().string(containsString("<span class=\"count-number\">25</span>")))
                .andExpect(content().string(containsString("<a href=\"/\">Тесты</a>")))
                .andExpect(content().string(containsString("href=\"/courses/1\" aria-current=\"page\">Java Core</a>")))
                .andExpect(content().string(containsString("Начать тестирование")))
                .andExpect(content().string(containsString("action=\"/courses/1/test\"")))
                .andExpect(content().string(containsString("Основы языка · Final")));
    }

    @Test
    void lessonSectionFiltersQuestionsAndLinksEveryAncestor() throws Exception {
        given(courseService.getQuestions(ScopeType.LESSON, 101L)).willReturn(List.of(FINAL_QUESTION));
        mvc.perform(get("/lessons/101"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a href=\"/courses/1\">Java Core</a>")))
                .andExpect(content().string(containsString("<a href=\"/topics/11\">Основы языка</a>")))
                .andExpect(content().string(containsString("Что означает final для класса?")))
                .andExpect(content().string(containsString("Все (4)")));
    }

    @Test
    void unknownSectionReturns404() throws Exception {
        mvc.perform(get("/lessons/999")).andExpect(status().isNotFound());
        mvc.perform(get("/topics/999/test")).andExpect(status().isNotFound());
    }

    @Test
    void legacyTopicQuizUrlRedirectsToTheTopicSection() throws Exception {
        mvc.perform(get("/topics/11/quiz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/topics/11"));
    }

    @Test
    void testPageEmbedsTheRandomizedQuestionsForTheWizard() throws Exception {
        given(courseService.getRandomizedQuestions(ScopeType.TOPIC, 11L, 10)).willReturn(List.of(FINAL_QUESTION));
        mvc.perform(get("/topics/11/test").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("test"))
                .andExpect(content().string(containsString("Тестирование")))
                .andExpect(content().string(containsString("\"secondsPerQuestion\":120")))
                .andExpect(content().string(containsString("\"lesson\":\"Final\"")))
                .andExpect(content().string(containsString("Сдать тест")))
                .andExpect(content().string(containsString("/prompts/java-test-agent.md")));
    }

    @Test
    void quizApiReturnsQuestionsWithTopicLessonAndAnswer() throws Exception {
        given(courseService.getRandomizedQuestions(eq(ScopeType.TOPIC), eq(11L), any())).willReturn(List.of(FINAL_QUESTION));
        mvc.perform(get("/api/topics/11/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("topics"))
                .andExpect(jsonPath("$.title").value("Java Core / Основы языка"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.questions[0].topic").value("Основы языка"))
                .andExpect(jsonPath("$.questions[0].lesson").value("Final"))
                .andExpect(jsonPath("$.questions[0].answer").value("Класс нельзя наследовать (например, String)."));
    }

    @Test
    void quizApiReturnsAnEmptyListForAPlaceholderTopic() throws Exception {
        given(courseService.getRandomizedQuestions(eq(ScopeType.TOPIC), anyLong(), any())).willReturn(List.of());
        mvc.perform(get("/api/topics/12/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }
}
