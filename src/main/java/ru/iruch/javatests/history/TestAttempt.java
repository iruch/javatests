package ru.iruch.javatests.history;

import jakarta.persistence.*;
import ru.iruch.javatests.course.ScopeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private ScopeType scopeType;
    @Column(name = "scope_id", nullable = false)
    private long scopeId;
    @Column(name = "course_title", nullable = false, length = 160)
    private String courseTitle;
    @Column(name = "scope_path", nullable = false, length = 600)
    private String scopePath;
    @Column(name = "covered_sections", nullable = false, columnDefinition = "TEXT")
    private String coveredSections;
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;
    @Column(name = "seconds_per_question", nullable = false)
    private int secondsPerQuestion;
    @Column(name = "question_count", nullable = false)
    private int questionCount;
    @Column(name = "answered_count", nullable = false)
    private int answeredCount;
    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;
    @Column(name = "viewed_count", nullable = false)
    private int viewedCount;
    @Column(name = "total_time_sec", nullable = false)
    private int totalTimeSec;
    @Column(name = "failed_question_number")
    private Integer failedQuestionNumber;
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    private List<TestAttemptAnswer> answers = new ArrayList<>();
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestAttemptFile> files = new ArrayList<>();

    protected TestAttempt() {}

    public TestAttempt(AttemptStatus status, ScopeType scopeType, long scopeId, String courseTitle, String scopePath,
                       String coveredSections, String timeZone, Instant startedAt, Instant finishedAt,
                       int secondsPerQuestion, int questionCount, int answeredCount, int viewedCount,
                       int totalTimeSec, Integer failedQuestionNumber) {
        this.status = status;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.courseTitle = courseTitle;
        this.scopePath = scopePath;
        this.coveredSections = coveredSections;
        this.timeZone = timeZone;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.secondsPerQuestion = secondsPerQuestion;
        this.questionCount = questionCount;
        this.answeredCount = answeredCount;
        this.skippedCount = questionCount - answeredCount;
        this.viewedCount = viewedCount;
        this.totalTimeSec = totalTimeSec;
        this.failedQuestionNumber = failedQuestionNumber;
    }

    public void addAnswer(TestAttemptAnswer answer) {
        answer.attachTo(this);
        answers.add(answer);
    }

    public void addFile(TestAttemptFile file) {
        file.attachTo(this);
        files.add(file);
    }

    public Long getId() { return id; }
    public AttemptStatus getStatus() { return status; }
    public ScopeType getScopeType() { return scopeType; }
    public long getScopeId() { return scopeId; }
    public String getCourseTitle() { return courseTitle; }
    public String getScopePath() { return scopePath; }
    public String getCoveredSections() { return coveredSections; }
    public String getTimeZone() { return timeZone; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getSecondsPerQuestion() { return secondsPerQuestion; }
    public int getQuestionCount() { return questionCount; }
    public int getAnsweredCount() { return answeredCount; }
    public int getSkippedCount() { return skippedCount; }
    public int getViewedCount() { return viewedCount; }
    public int getTotalTimeSec() { return totalTimeSec; }
    public Integer getFailedQuestionNumber() { return failedQuestionNumber; }
    public List<TestAttemptAnswer> getAnswers() { return answers; }
    public List<TestAttemptFile> getFiles() { return files; }
}
