package ru.iruch.javatests.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("""
            select q from Question q join fetch q.lesson l
            where l.topic.id = :topicId and l.active = true and q.active = true
            """)
    List<Question> findActiveByTopicId(long topicId);

    @Query("""
            select l.topic.id as topicId, count(q) as total from Question q join q.lesson l
            where l.active = true and q.active = true
            group by l.topic.id
            """)
    List<TopicQuestionCount> countActiveByTopic();

    interface TopicQuestionCount {
        Long getTopicId();
        long getTotal();
    }
}
