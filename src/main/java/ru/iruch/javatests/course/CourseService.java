package ru.iruch.javatests.course;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;

    public CourseService(CourseRepository courseRepository, TopicRepository topicRepository) {
        this.courseRepository = courseRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseView> getCourses() {
        return courseRepository.findAllByOrderBySortOrderAsc().stream()
                .map(course -> new CourseView(course.getId(), course.getSlug(), course.getTitle(), course.getTopics().stream()
                        .map(topic -> new TopicView(topic.getId(), topic.getTitle(), topic.getLessons().size()))
                        .toList()))
                .toList();
    }

    public Topic getTopic(long id) {
        return topicRepository.findWithLessonsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    public List<Lesson> getRandomizedLessons(long id) {
        return getRandomizedLessons(getTopic(id));
    }

    public List<Lesson> getRandomizedLessons(Topic topic) {
        List<Lesson> lessons = new ArrayList<>(topic.getLessons().stream().filter(Lesson::isActive).toList());
        Collections.shuffle(lessons);
        return lessons;
    }

    public record CourseView(Long id, String slug, String title, List<TopicView> topics) {}
    public record TopicView(Long id, String title, int lessonCount) {}
}
