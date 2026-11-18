-- Случайная выборка вопросов для тренировки.
-- Переменные psql: topic (часть названия темы), lesson (часть названия урока), n (сколько вопросов).
-- Пример:
--   docker compose exec -T postgres psql -U javatests -d javatests \
--     -v topic='Основы' -v lesson='' -v n=10 < scripts/drill.sql
SELECT t.title AS topic, l.title AS lesson, q.id, q.question_text, q.answer_text
FROM questions q
JOIN lessons l ON l.id = q.lesson_id
JOIN topics t ON t.id = l.topic_id
WHERE q.active AND l.active
  AND t.title ILIKE '%' || :'topic' || '%'
  AND l.title ILIKE '%' || :'lesson' || '%'
ORDER BY random()
LIMIT :n;
