-- Урок (lesson) становится подтемой, а контрольные вопросы с ответами живут в questions.
ALTER TABLE lessons RENAME COLUMN question_text TO description;
ALTER TABLE lessons ALTER COLUMN description DROP NOT NULL;
ALTER TABLE lessons ADD CONSTRAINT uq_lessons_topic_title UNIQUE (topic_id, title);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    answer_text TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL
);

CREATE INDEX idx_questions_lesson_order ON questions(lesson_id, sort_order);
