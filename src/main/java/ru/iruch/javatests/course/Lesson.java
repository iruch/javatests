package ru.iruch.javatests.course;

import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    @Column(nullable = false, length = 240)
    private String title;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Lesson() {}
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getQuestionText() { return questionText; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
