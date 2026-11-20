package ru.iruch.javatests.course;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public enum ScopeType {
    COURSE("courses"),
    TOPIC("topics"),
    LESSON("lessons");

    public static final String SEGMENT_PATTERN = "courses|topics|lessons";

    private final String segment;

    ScopeType(String segment) { this.segment = segment; }

    public String segment() { return segment; }

    public String url(long id) { return "/" + segment + "/" + id; }

    public static ScopeType fromSegment(String segment) {
        return Arrays.stream(values())
                .filter(type -> type.segment.equals(segment))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown section"));
    }
}
