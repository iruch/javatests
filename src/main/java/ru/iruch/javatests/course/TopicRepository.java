package ru.iruch.javatests.course;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @EntityGraph(attributePaths = {"course", "lessons"})
    Optional<Topic> findWithLessonsById(Long id);
}
