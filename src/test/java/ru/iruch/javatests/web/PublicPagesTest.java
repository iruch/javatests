package ru.iruch.javatests.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.iruch.javatests.course.CourseService;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HomeController.class, QuizApiController.class})
class PublicPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean CourseService courseService;

    @Test
    void homeIsPublicAndRendersTheCourseLibrary() throws Exception {
        given(courseService.getCourses()).willReturn(List.of(new CourseService.CourseView(
                1L, "java-core", "Java Core", List.of(new CourseService.TopicView(11L, "Основы языка", 0)))));
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("JavaTests")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Библиотека тестов")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java Core")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Основы языка")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/topics/11/quiz")));
    }

    @Test
    void quizApiReturnsAnEmptyQuestionListForAPlaceholderTopic() throws Exception {
        var topic = org.mockito.Mockito.mock(ru.iruch.javatests.course.Topic.class);
        given(topic.getId()).willReturn(42L);
        given(topic.getTitle()).willReturn("Основы языка");
        given(courseService.getTopic(42L)).willReturn(topic);
        given(courseService.getRandomizedQuestions(topic)).willReturn(List.of());
        mvc.perform(get("/api/topics/42/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(42))
                .andExpect(jsonPath("$.topic").value("Основы языка"))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void quizApiReturnsQuestionsWithLessonAndAnswer() throws Exception {
        var topic = org.mockito.Mockito.mock(ru.iruch.javatests.course.Topic.class);
        given(topic.getId()).willReturn(7L);
        given(topic.getTitle()).willReturn("Основы языка");
        given(courseService.getTopic(7L)).willReturn(topic);
        given(courseService.getRandomizedQuestions(topic)).willReturn(List.of(new CourseService.QuestionView(
                1L, "Final", "Что означает final для класса?", "Класс нельзя наследовать (например, String).")));
        mvc.perform(get("/api/topics/7/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.questions[0].lesson").value("Final"))
                .andExpect(jsonPath("$.questions[0].question").value("Что означает final для класса?"))
                .andExpect(jsonPath("$.questions[0].answer").value("Класс нельзя наследовать (например, String)."));
    }
}
