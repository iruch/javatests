package ru.iruch.javatests.history;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

public enum ReportVariant {
    FULL("full", "полный", "prompts/java-test-agent-full.md"),
    SHORT("short", "упрощённый", "prompts/java-test-agent-short.md");

    private final String slug;
    private final String label;
    private final String template;

    ReportVariant(String slug, String label, String template) {
        this.slug = slug;
        this.label = label;
        this.template = template;
    }

    public String slug() { return slug; }

    public String label() { return label; }

    public String template() { return template; }

    public static ReportVariant fromSlug(String slug) {
        for (ReportVariant variant : values()) {
            if (variant.slug.equals(slug.toLowerCase(Locale.ROOT))) return variant;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report variant");
    }

    public enum Format {
        MD("md", "text/markdown"),
        JSON("json", "application/json");

        private final String extension;
        private final String mediaType;

        Format(String extension, String mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        public String extension() { return extension; }

        public String mediaType() { return mediaType; }

        public static Format fromExtension(String extension) {
            for (Format format : values()) {
                if (format.extension.equals(extension.toLowerCase(Locale.ROOT))) return format;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown file format");
        }
    }
}
