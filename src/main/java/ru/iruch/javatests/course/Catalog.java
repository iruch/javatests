package ru.iruch.javatests.course;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of the navigation tree with active question counts; pages resolve their scope from it
 * so the sidebar, breadcrumbs and counters always agree.
 */
public record Catalog(List<CourseView> courses) {
    private static final int[] TEST_SIZES = {10, 20, 50};
    private static final int DEFAULT_TEST_SIZE = 20;

    public int topicCount() {
        return courses.stream().mapToInt(course -> course.topics().size()).sum();
    }

    public int questionCount() {
        return courses.stream().mapToInt(CourseView::questionCount).sum();
    }

    public ScopeView scope(ScopeType type, long id) {
        for (CourseView course : courses) {
            if (type == ScopeType.COURSE && course.id() == id) {
                return new ScopeView(type, id, course.title(), "Курс", course.questionCount(),
                        List.of(home(), crumb(ScopeType.COURSE, course.id(), course.title())),
                        course.topics().stream()
                                .map(topic -> new Link(topic.title(), ScopeType.TOPIC.url(topic.id()), topic.questionCount()))
                                .toList(),
                        course.id(), null, null);
            }
            for (TopicView topic : course.topics()) {
                if (type == ScopeType.TOPIC && topic.id() == id) {
                    return new ScopeView(type, id, topic.title(), course.title(), topic.questionCount(),
                            List.of(home(), crumb(ScopeType.COURSE, course.id(), course.title()),
                                    crumb(ScopeType.TOPIC, topic.id(), topic.title())),
                            topic.lessons().stream()
                                    .map(lesson -> new Link(lesson.title(), ScopeType.LESSON.url(lesson.id()), lesson.questionCount()))
                                    .toList(),
                            course.id(), topic.id(), null);
                }
                for (LessonView lesson : topic.lessons()) {
                    if (type == ScopeType.LESSON && lesson.id() == id) {
                        return new ScopeView(type, id, lesson.title(), course.title() + " · " + topic.title(),
                                lesson.questionCount(),
                                List.of(home(), crumb(ScopeType.COURSE, course.id(), course.title()),
                                        crumb(ScopeType.TOPIC, topic.id(), topic.title()),
                                        crumb(ScopeType.LESSON, lesson.id(), lesson.title())),
                                List.of(), course.id(), topic.id(), lesson.id());
                    }
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found");
    }

    private static Crumb home() { return new Crumb("Тесты", "/"); }

    private static Crumb crumb(ScopeType type, long id, String title) { return new Crumb(title, type.url(id)); }

    public record CourseView(Long id, String slug, String title, int questionCount, List<TopicView> topics) {}

    public record TopicView(Long id, String title, int questionCount, List<LessonView> lessons) {}

    public record LessonView(Long id, String title, int questionCount) {}

    public record Crumb(String title, String url) {}

    public record Link(String title, String url, int questionCount) {}

    public record ScopeView(ScopeType type, long id, String title, String eyebrow, int questionCount,
                            List<Crumb> crumbs, List<Link> children,
                            Long courseId, Long topicId, Long lessonId) {
        public String url() { return type.url(id); }

        public String testUrl() { return url() + "/test"; }

        public String path() {
            return String.join(" / ", crumbs.stream().skip(1).map(Crumb::title).toList());
        }

        public List<Integer> testSizes() {
            List<Integer> sizes = new ArrayList<>();
            for (int size : TEST_SIZES) {
                if (size < questionCount) sizes.add(size);
            }
            return sizes;
        }

        public int defaultTestSize() { return Math.min(DEFAULT_TEST_SIZE, questionCount); }
    }
}
