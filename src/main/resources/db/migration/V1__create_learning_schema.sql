CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    question_text TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL
);

CREATE INDEX idx_topics_course_order ON topics(course_id, sort_order);
CREATE INDEX idx_lessons_topic_order ON lessons(topic_id, sort_order);

INSERT INTO courses (slug, title, sort_order) VALUES
    ('java-core', 'Java Core', 10),
    ('spring-framework-part-one', 'Spring Framework part one', 20);

INSERT INTO topics (course_id, title, sort_order)
SELECT id, topic.title, topic.sort_order
FROM courses c
JOIN (VALUES
    ('java-core', 'Основы языка', 10),
    ('java-core', 'Особенности внутренней работы Java', 20),
    ('java-core', 'Java Collection Framework', 30),
    ('java-core', 'Java Stream API', 40),
    ('java-core', 'Java Concurrency', 50),
    ('java-core', 'Системы сборки', 60),
    ('java-core', 'Паттерны проектирования', 70),
    ('java-core', 'Java Core практика', 80),
    ('spring-framework-part-one', 'Spring Core', 10),
    ('spring-framework-part-one', 'Spring MVC', 20),
    ('spring-framework-part-one', 'Spring Data', 30)
) AS topic(slug, title, sort_order) ON topic.slug = c.slug;
