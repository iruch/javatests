# JavaTests project contracts

## Product

JavaTests is a public, unauthenticated learning site. The left navigation exposes a
collapsible hierarchy of courses and topics. Selecting a topic opens its quiz in the
main area: all active questions of the topic's lessons, shuffled, each with a collapsible
answer.

## HTTP contract

- `GET /` renders the public course navigator and an empty-state panel.
- `GET /topics/{topicId}/quiz` renders a topic quiz page.
- `GET /api/topics/{topicId}/quiz` returns `{ topicId, topic, questions, total }` where each
  question is `{ id, lesson, question, answer }`; questions are shuffled on every request.
  The endpoint is read-only and public.
- Unknown course/topic ids return HTTP 404.
- The application listens on port `8097` by default.

## Data contract

- PostgreSQL is the runtime database. Flyway owns every schema change and seed record.
- The relational hierarchy is `courses -> topics -> lessons -> questions`.
  - A lesson is a sub-topic (e.g. "Final", "String pool"); `title` is unique per topic.
  - A question holds `question_text` and its reference `answer_text`.
  - Lessons and questions may be inactive while content is being prepared.
- Ordering is explicit (`sort_order`); display titles are data, not hard-coded in templates.
- Deleting a course cascades to its topics, lessons and questions.

## Content sources

- Each topic's questions are seeded by its own migration (e.g.
  `V3__seed_java_core_basics_questions.sql` for Java Core / Основы языка). The migration
  fills temp tables `seed_lessons` / `seed_questions` and fails if a question references an
  unknown lesson or a lesson has no questions.

## Drill mode (Claude as interviewer)

When asked "погоняй меня по теме X", pull random questions from the database rather than
inventing them:

```bash
docker compose up -d postgres
docker compose exec -T postgres psql -U javatests -d javatests -v topic='Основы' -v lesson='' -v n=10 < scripts/drill.sql
```

Ask one question at a time, wait for the answer, compare it with `answer_text` (plus
general Java knowledge for nuance), point out gaps, then move on.

## Delivery contract

- Main development happens on `dev`; merge into `main` only when the user requests it.
- Keep the public site free of authentication and registration flows.
- Prefer server-side Thymeleaf rendering and lightweight dependencies suitable for a
  1 vCPU / 2 GB RAM VPS.
- New schema or seed changes must be a new versioned Flyway migration.
