package ru.iruch.javatests.course;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    public CourseService(CourseRepository courseRepository, TopicRepository topicRepository,
                         QuestionRepository questionRepository) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseView> getCourses() {
        Map<Long, Long> questionCounts = questionRepository.countActiveByTopic().stream()
                .collect(Collectors.toMap(QuestionRepository.TopicQuestionCount::getTopicId,
                        QuestionRepository.TopicQuestionCount::getTotal));
        return courseRepository.findAllByOrderBySortOrderAsc().stream()
                .map(course -> new CourseView(course.getId(), course.getSlug(), course.getTitle(), course.getTopics().stream()
                        .map(topic -> new TopicView(topic.getId(), topic.getTitle(),
                                questionCounts.getOrDefault(topic.getId(), 0L).intValue()))
                        .toList()))
                .toList();
    }

    public Topic getTopic(long id) {
        return topicRepository.findWithLessonsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    @Transactional(readOnly = true)
    public List<QuestionView> getRandomizedQuestions(Topic topic) {
        List<QuestionView> questions = new ArrayList<>(questionRepository.findActiveByTopicId(topic.getId()).stream()
                .map(q -> new QuestionView(q.getId(), q.getLesson().getTitle(), q.getQuestionText(), q.getAnswerText()))
                .toList());
        Collections.shuffle(questions);
        return questions;
    }

    public record CourseView(Long id, String slug, String title, List<TopicView> topics) {}
    public record TopicView(Long id, String title, int questionCount) {}
    public record QuestionView(Long id, String lesson, String question, String answer) {}
}
