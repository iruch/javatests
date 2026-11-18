package ru.iruch.javatests.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.iruch.javatests.course.CourseService;
import java.util.List;

@RestController
public class QuizApiController {
    private final CourseService courseService;
    public QuizApiController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/api/topics/{topicId}/quiz")
    public QuizResponse quiz(@PathVariable long topicId) {
        var topic = courseService.getTopic(topicId);
        var questions = courseService.getRandomizedQuestions(topic);
        return new QuizResponse(topic.getId(), topic.getTitle(), questions, questions.size());
    }

    public record QuizResponse(Long topicId, String topic, List<CourseService.QuestionView> questions, int total) {}
}
