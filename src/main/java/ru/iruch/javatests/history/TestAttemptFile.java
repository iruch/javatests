package ru.iruch.javatests.history;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_attempt_files")
public class TestAttemptFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TestAttempt attempt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportVariant variant;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportVariant.Format format;
    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected TestAttemptFile() {}

    public TestAttemptFile(ReportVariant variant, ReportVariant.Format format, String fileName, String content) {
        this.variant = variant;
        this.format = format;
        this.fileName = fileName;
        this.content = content;
    }

    void attachTo(TestAttempt attempt) { this.attempt = attempt; }

    public Long getId() { return id; }
    public ReportVariant getVariant() { return variant; }
    public ReportVariant.Format getFormat() { return format; }
    public String getFileName() { return fileName; }
    public String getContent() { return content; }
}
