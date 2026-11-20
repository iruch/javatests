package ru.iruch.javatests.history;

public enum AttemptStatus {
    /** Finished with "Сдать тест": answers and report files are stored. */
    SUBMITTED,
    /** A question ran out of time: the test was reset, only the summary is stored. */
    FAILED
}
