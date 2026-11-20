package ru.iruch.javatests.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findAllByOrderByFinishedAtDescIdDesc();

    @Query("select a from TestAttemptAnswer a where a.attempt.id in :attemptIds order by a.attempt.id, a.number")
    List<TestAttemptAnswer> findAnswersByAttemptIds(Collection<Long> attemptIds);

    @Query("""
            select f from TestAttemptFile f
            where f.attempt.id = :attemptId and f.variant = :variant and f.format = :format
            """)
    Optional<TestAttemptFile> findFile(long attemptId, ReportVariant variant, ReportVariant.Format format);

    /** Answers and files go with the attempt through ON DELETE CASCADE in the database. */
    @Modifying
    @Query("delete from TestAttempt a where a.id = :id")
    int deleteAttemptById(long id);
}
