package ru.iruch.javatests.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.iruch.javatests.course.CourseService;

@Controller
public class HomeController {
    private final CourseService courseService;
    public HomeController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/")
    public String home(Model model) {
        var courses = courseService.getCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("topicCount", courses.stream().mapToInt(course -> course.topics().size()).sum());
        return "index";
    }

    @GetMapping("/topics/{topicId}/quiz")
    public String quiz(@PathVariable long topicId, Model model) {
        var topic = courseService.getTopic(topicId);
        var courses = courseService.getCourses();
        model.addAttribute("courses", courses);
        model.addAttribute("topicCount", courses.stream().mapToInt(course -> course.topics().size()).sum());
        model.addAttribute("selectedTopicId", topicId);
        model.addAttribute("topic", topic);
        model.addAttribute("lessons", courseService.getRandomizedLessons(topic));
        return "quiz";
    }
}
