-- История тестирования: каждая попытка, её вопросы с ответами и сгенерированные файлы для ИИ-агента.
-- Записи не персонализированы: это история одной песочницы (одной базы).

CREATE TABLE test_attempts (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUBMITTED', 'FAILED')),
    scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('COURSE', 'TOPIC', 'LESSON')),
    scope_id BIGINT NOT NULL,
    course_title VARCHAR(160) NOT NULL,
    scope_path VARCHAR(600) NOT NULL,
    covered_sections TEXT NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    seconds_per_question INTEGER NOT NULL,
    question_count INTEGER NOT NULL,
    answered_count INTEGER NOT NULL,
    skipped_count INTEGER NOT NULL,
    viewed_count INTEGER NOT NULL,
    total_time_sec INTEGER NOT NULL,
    failed_question_number INTEGER
);

CREATE INDEX idx_test_attempts_finished ON test_attempts(finished_at DESC);

-- Вопрос и эталон копируются в попытку: история не меняется, если банк вопросов правят позже.
CREATE TABLE test_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    number INTEGER NOT NULL,
    question_id BIGINT REFERENCES questions(id) ON DELETE SET NULL,
    topic_title VARCHAR(160) NOT NULL,
    lesson_title VARCHAR(240) NOT NULL,
    question_text TEXT NOT NULL,
    reference_answer TEXT NOT NULL,
    user_answer TEXT NOT NULL,
    answer_viewed BOOLEAN NOT NULL,
    time_spent_sec INTEGER NOT NULL,
    CONSTRAINT uq_test_attempt_answers_number UNIQUE (attempt_id, number)
);

CREATE TABLE test_attempt_files (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    variant VARCHAR(10) NOT NULL CHECK (variant IN ('FULL', 'SHORT')),
    format VARCHAR(10) NOT NULL CHECK (format IN ('MD', 'JSON')),
    file_name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    CONSTRAINT uq_test_attempt_files UNIQUE (attempt_id, variant, format)
);
