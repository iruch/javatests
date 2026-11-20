package ru.iruch.javatests.course;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String slug;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Topic> topics = new ArrayList<>();

    protected Course() {}
    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public int getSortOrder() { return sortOrder; }
    public List<Topic> getTopics() { return topics; }
}
