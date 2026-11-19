package ru.iruch.javatests.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    public CourseService(CourseRepository courseRepository, LessonRepository lessonRepository,
                         QuestionRepository questionRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public Catalog getCatalog() {
        Map<Long, Long> questionCounts = questionRepository.countActiveByLesson().stream()
                .collect(Collectors.toMap(QuestionRepository.LessonQuestionCount::getLessonId,
                        QuestionRepository.LessonQuestionCount::getTotal));
        Map<Long, List<Catalog.LessonView>> lessonsByTopic = lessonRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getTopic().getId(),
                        Collectors.mapping(lesson -> new Catalog.LessonView(lesson.getId(), lesson.getTitle(),
                                questionCounts.getOrDefault(lesson.getId(), 0L).intValue()), Collectors.toList())));
        return new Catalog(courseRepository.findAllByOrderBySortOrderAsc().stream()
                .map(course -> {
                    List<Catalog.TopicView> topics = course.getTopics().stream()
                            .map(topic -> {
                                List<Catalog.LessonView> lessons = lessonsByTopic.getOrDefault(topic.getId(), List.of());
                                return new Catalog.TopicView(topic.getId(), topic.getTitle(),
                                        lessons.stream().mapToInt(Catalog.LessonView::questionCount).sum(), lessons);
                            })
                            .toList();
                    return new Catalog.CourseView(course.getId(), course.getSlug(), course.getTitle(),
                            topics.stream().mapToInt(Catalog.TopicView::questionCount).sum(), topics);
                })
                .toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionView> getQuestions(ScopeType type, long id) {
        List<Question> questions = switch (type) {
            case COURSE -> questionRepository.findActiveByCourseId(id);
            case TOPIC -> questionRepository.findActiveByTopicId(id);
            case LESSON -> questionRepository.findActiveByLessonId(id);
        };
        return questions.stream().map(QuestionView::of).toList();
    }

    /** Shuffled questions of the scope; {@code limit} outside 1..size means "all of them". */
    @Transactional(readOnly = true)
    public List<QuestionView> getRandomizedQuestions(ScopeType type, long id, Integer limit) {
        List<QuestionView> questions = new ArrayList<>(getQuestions(type, id));
        Collections.shuffle(questions);
        if (limit == null || limit <= 0 || limit >= questions.size()) {
            return questions;
        }
        return List.copyOf(questions.subList(0, limit));
    }

    public record QuestionView(Long id, String topic, String lesson, String question, String answer) {
        static QuestionView of(Question question) {
            return new QuestionView(question.getId(), question.getLesson().getTopic().getTitle(),
                    question.getLesson().getTitle(), question.getQuestionText(), question.getAnswerText());
        }
    }
}
