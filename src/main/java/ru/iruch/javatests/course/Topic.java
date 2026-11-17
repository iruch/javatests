package ru.iruch.javatests.course;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Lesson> lessons = new ArrayList<>();

    protected Topic() {}
    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public int getSortOrder() { return sortOrder; }
    public List<Lesson> getLessons() { return lessons; }
}
