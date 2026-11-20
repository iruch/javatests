package ru.iruch.javatests.history;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_attempt_answers")
public class TestAttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TestAttempt attempt;
    @Column(nullable = false)
    private int number;
    @Column(name = "question_id")
    private Long questionId;
    @Column(name = "topic_title", nullable = false, length = 160)
    private String topicTitle;
    @Column(name = "lesson_title", nullable = false, length = 240)
    private String lessonTitle;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    @Column(name = "reference_answer", nullable = false, columnDefinition = "TEXT")
    private String referenceAnswer;
    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;
    @Column(name = "answer_viewed", nullable = false)
    private boolean answerViewed;
    @Column(name = "time_spent_sec", nullable = false)
    private int timeSpentSec;

    protected TestAttemptAnswer() {}

    public TestAttemptAnswer(int number, Long questionId, String topicTitle, String lessonTitle, String questionText,
                             String referenceAnswer, String userAnswer, boolean answerViewed, int timeSpentSec) {
        this.number = number;
        this.questionId = questionId;
        this.topicTitle = topicTitle;
        this.lessonTitle = lessonTitle;
        this.questionText = questionText;
        this.referenceAnswer = referenceAnswer;
        this.userAnswer = userAnswer;
        this.answerViewed = answerViewed;
        this.timeSpentSec = timeSpentSec;
    }

    void attachTo(TestAttempt attempt) { this.attempt = attempt; }

    public Long getId() { return id; }
    public TestAttempt getAttempt() { return attempt; }
    public int getNumber() { return number; }
    public Long getQuestionId() { return questionId; }
    public String getTopicTitle() { return topicTitle; }
    public String getLessonTitle() { return lessonTitle; }
    public String getQuestionText() { return questionText; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public String getUserAnswer() { return userAnswer; }
    public boolean isAnswerViewed() { return answerViewed; }
    public int getTimeSpentSec() { return timeSpentSec; }
}
