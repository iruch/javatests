package ru.iruch.javatests.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.iruch.javatests.course.CourseService;
import ru.iruch.javatests.history.AttemptStatus;
import ru.iruch.javatests.history.ReportVariant;
import ru.iruch.javatests.history.TestHistoryService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HistoryController {
    private final CourseService courseService;
    private final TestHistoryService historyService;

    public HistoryController(CourseService courseService, TestHistoryService historyService) {
        this.courseService = courseService;
        this.historyService = historyService;
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("catalog", courseService.getCatalog());
        model.addAttribute("attempts", historyService.getHistory());
        model.addAttribute("activeNav", "history");
        return "history";
    }

    @PostMapping("/history/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        historyService.delete(id);
        redirect.addFlashAttribute("deletedNotice", "Тест удалён вместе с ответами и файлами результата.");
        return "redirect:/history";
    }

    @GetMapping("/history/{id}/download/{variant}/{format}")
    public ResponseEntity<byte[]> download(@PathVariable long id, @PathVariable String variant, @PathVariable String format) {
        var file = historyService.getFile(id, ReportVariant.fromSlug(variant), ReportVariant.Format.fromExtension(format));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getFileName()).build().toString())
                .contentType(new MediaType(MediaType.parseMediaType(file.getFormat().mediaType()), StandardCharsets.UTF_8))
                .body(file.getContent().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/api/test-attempts")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse record(@Valid @RequestBody TestHistoryService.AttemptRequest request, HttpServletRequest http) {
        var recorded = historyService.record(request);
        String base = http.getContextPath() + "/history";
        Map<String, String> downloads = new LinkedHashMap<>();
        if (recorded.status() == AttemptStatus.SUBMITTED) {
            for (ReportVariant variant : ReportVariant.values()) {
                downloads.put(variant.slug(), base + "/" + recorded.id() + "/download/" + variant.slug());
            }
        }
        return new AttemptResponse(recorded, base + "#attempt-" + recorded.id(), downloads);
    }

    /** {@code downloads} maps a report variant to its base URL; append "/md" or "/json" for the files. */
    public record AttemptResponse(TestHistoryService.RecordedAttempt attempt, String historyUrl,
                                  Map<String, String> downloads) {}
}
