package ru.iruch.javatests.web;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.iruch.javatests.course.CourseService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Renders the project README (copied to the classpath at build time) as the usage guide. */
@Controller
public class HelpController {
    static final String README = "help/README.md";

    private final CourseService courseService;
    private final String helpHtml;

    public HelpController(CourseService courseService) {
        this.courseService = courseService;
        this.helpHtml = render(readReadme());
    }

    @GetMapping("/help")
    public String help(Model model) {
        model.addAttribute("catalog", courseService.getCatalog());
        model.addAttribute("helpHtml", helpHtml);
        model.addAttribute("activeNav", "help");
        return "help";
    }

    static String render(String markdown) {
        var extensions = List.of(TablesExtension.create());
        var parser = Parser.builder().extensions(extensions).build();
        var renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build();
        return renderer.render(parser.parse(markdown));
    }

    private static String readReadme() {
        var resource = new ClassPathResource(README);
        if (!resource.exists()) {
            return "# Справка недоступна\n\nREADME.md не попал в сборку: запустите `mvn package` из корня проекта.";
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# Справка недоступна\n\nНе удалось прочитать README.md: " + e.getMessage();
        }
    }
}
