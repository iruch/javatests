package ru.iruch.javatests.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.course.ScopeType;

import java.util.List;

@Controller
public class HomeController {
    static final int SECONDS_PER_QUESTION = 120;

    private final CourseService courseService;

    public HomeController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("catalog", courseService.getCatalog());
        return "index";
    }

    @GetMapping("/{segment:" + ScopeType.SEGMENT_PATTERN + "}/{id}")
    public String section(@PathVariable String segment, @PathVariable long id, Model model) {
        var type = ScopeType.fromSegment(segment);
        var catalog = courseService.getCatalog();
        model.addAttribute("catalog", catalog);
        model.addAttribute("scope", catalog.scope(type, id));
        model.addAttribute("questions", courseService.getQuestions(type, id));
        return "section";
    }

    @GetMapping("/{segment:" + ScopeType.SEGMENT_PATTERN + "}/{id}/test")
    public String test(@PathVariable String segment, @PathVariable long id,
                       @RequestParam(required = false) Integer limit, Model model) {
        var type = ScopeType.fromSegment(segment);
        var catalog = courseService.getCatalog();
        var scope = catalog.scope(type, id);
        var questions = courseService.getRandomizedQuestions(type, id, limit);
        model.addAttribute("catalog", catalog);
        model.addAttribute("scope", scope);
        model.addAttribute("test", new TestView(scope.path(), scope.url(), SECONDS_PER_QUESTION, questions));
        return "test";
    }

    @GetMapping("/topics/{id}/quiz")
    public String legacyQuiz(@PathVariable long id) {
        return "redirect:" + ScopeType.TOPIC.url(id);
    }

    public record TestView(String title, String backUrl, int secondsPerQuestion,
                           List<CourseService.QuestionView> questions) {}
}
