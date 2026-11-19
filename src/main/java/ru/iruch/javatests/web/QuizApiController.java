package ru.iruch.javatests.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.course.ScopeType;

import java.util.List;

@RestController
public class QuizApiController {
    private final CourseService courseService;

    public QuizApiController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/api/{segment:" + ScopeType.SEGMENT_PATTERN + "}/{id}/quiz")
    public QuizResponse quiz(@PathVariable String segment, @PathVariable long id,
                             @RequestParam(required = false) Integer limit) {
        var type = ScopeType.fromSegment(segment);
        var scope = courseService.getCatalog().scope(type, id);
        var questions = courseService.getRandomizedQuestions(type, id, limit);
        return new QuizResponse(type.segment(), id, scope.path(), questions, questions.size());
    }

    public record QuizResponse(String scope, long id, String title, List<CourseService.QuestionView> questions, int total) {}
}
