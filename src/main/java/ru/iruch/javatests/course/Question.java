package ru.iruch.javatests.course;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Question() {}
    public Long getId() { return id; }
    public Lesson getLesson() { return lesson; }
    public String getQuestionText() { return questionText; }
    public String getAnswerText() { return answerText; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
