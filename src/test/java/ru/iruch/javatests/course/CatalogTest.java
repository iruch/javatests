package ru.iruch.javatests.course;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {
    private final Catalog catalog = new Catalog(List.of(
            new Catalog.CourseView(1L, "java-core", "Java Core", 65, List.of(
                    new Catalog.TopicView(11L, "Основы языка", 65, List.of(
                            new Catalog.LessonView(101L, "Final", 5),
                            new Catalog.LessonView(102L, "Static", 60)))))));

    @Test
    void courseScopeCountsEveryQuestionAndListsTopics() {
        var scope = catalog.scope(ScopeType.COURSE, 1L);
        assertThat(scope.questionCount()).isEqualTo(65);
        assertThat(scope.crumbs()).extracting(Catalog.Crumb::url).containsExactly("/", "/courses/1");
        assertThat(scope.children()).extracting(Catalog.Link::url).containsExactly("/topics/11");
        assertThat(scope.testSizes()).containsExactly(10, 20, 50);
        assertThat(scope.defaultTestSize()).isEqualTo(20);
    }

    @Test
    void lessonScopeKnowsItsAncestors() {
        var scope = catalog.scope(ScopeType.LESSON, 101L);
        assertThat(scope.path()).isEqualTo("Java Core / Основы языка / Final");
        assertThat(scope.crumbs()).extracting(Catalog.Crumb::url)
                .containsExactly("/", "/courses/1", "/topics/11", "/lessons/101");
        assertThat(scope.testSizes()).isEmpty();
        assertThat(scope.defaultTestSize()).isEqualTo(5);
        assertThat(scope.testUrl()).isEqualTo("/lessons/101/test");
    }

    @Test
    void unknownIdOrWrongLevelIsNotFound() {
        assertThatThrownBy(() -> catalog.scope(ScopeType.TOPIC, 1L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> catalog.scope(ScopeType.LESSON, 999L)).isInstanceOf(ResponseStatusException.class);
    }
}
