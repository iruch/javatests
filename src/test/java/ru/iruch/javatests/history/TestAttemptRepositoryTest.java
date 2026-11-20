package ru.iruch.javatests.history;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.iruch.javatests.course.ScopeType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TestAttemptRepositoryTest {
    @Autowired TestAttemptRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test
    void deletingOneAttemptKeepsTheOthersWithTheirAnswersAndFiles() {
        long first = save("Final").getId();
        long second = save("Static").getId();
        long third = save("Object").getId();
        entityManager.clear();

        assertThat(repository.deleteAttemptById(second)).isEqualTo(1);
        entityManager.clear();

        assertThat(repository.findAll()).extracting(TestAttempt::getId).containsExactlyInAnyOrder(first, third);
        assertThat(count("select count(a) from TestAttemptAnswer a where a.attempt.id = " + second)).isZero();
        assertThat(count("select count(f) from TestAttemptFile f where f.attempt.id = " + second)).isZero();
        assertThat(count("select count(a) from TestAttemptAnswer a")).isEqualTo(2);
        assertThat(count("select count(f) from TestAttemptFile f")).isEqualTo(2);
    }

    @Test
    void deletingAMissingAttemptChangesNothing() {
        long kept = save("Final").getId();
        entityManager.clear();

        assertThat(repository.deleteAttemptById(kept + 1000)).isZero();
        assertThat(repository.findAll()).extracting(TestAttempt::getId).containsExactly(kept);
    }

    private TestAttempt save(String lesson) {
        var attempt = new TestAttempt(AttemptStatus.SUBMITTED, ScopeType.LESSON, 1L, "Java Core",
                "Java Core / Основы языка / " + lesson, "Основы языка: " + lesson, "Europe/Moscow",
                Instant.parse("2026-11-20T08:00:00Z"), Instant.parse("2026-11-20T08:01:00Z"), 120, 1, 1, 0, 60, null);
        attempt.addAnswer(new TestAttemptAnswer(1, null, "Основы языка", lesson, "Вопрос", "Эталон", "Ответ", false, 60));
        attempt.addFile(new TestAttemptFile(ReportVariant.FULL, ReportVariant.Format.MD, lesson + ".md", "# " + lesson));
        return entityManager.persistFlushFind(attempt);
    }

    private long count(String jpql) {
        return entityManager.getEntityManager().createQuery(jpql, Long.class).getSingleResult();
    }
}
