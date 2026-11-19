package ru.iruch.javatests.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
            select q from Question q join fetch q.lesson l join fetch l.topic t
            where t.course.id = :courseId and l.active = true and q.active = true
            order by t.sortOrder, l.sortOrder, q.sortOrder
            """)
    List<Question> findActiveByCourseId(long courseId);

    @Query("""
            select q from Question q join fetch q.lesson l join fetch l.topic t
            where t.id = :topicId and l.active = true and q.active = true
            order by l.sortOrder, q.sortOrder
            """)
    List<Question> findActiveByTopicId(long topicId);

    @Query("""
            select q from Question q join fetch q.lesson l join fetch l.topic t
            where l.id = :lessonId and l.active = true and q.active = true
            order by q.sortOrder
            """)
    List<Question> findActiveByLessonId(long lessonId);

    @Query("""
            select l.id as lessonId, count(q) as total from Question q join q.lesson l
            where l.active = true and q.active = true
            group by l.id
            """)
    List<LessonQuestionCount> countActiveByLesson();

    interface LessonQuestionCount {
        Long getLessonId();
        long getTotal();
    }
}
