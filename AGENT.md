# Java Tests project contracts

## Product

Java Tests is a public, unauthenticated learning site. The left navigation is a collapsible
tree `course -> topic -> lesson`, each node with its
active question count. Selecting any node opens a section page: linked breadcrumbs
(`Тесты / Java Core / Основы языка / Final`), the total question count of the node, its child
sections and its question bank (ordered, answers collapsible).

Every section has a "Начать тестирование" launcher (10 / 20 / 50 / all random questions). The
test is a client-side wizard (`static/js/test.js`): one question per step, "Назад" / "Далее",
"Сдать тест" on the last step. Each question has its own 2-minute budget that runs only while
the question is open; when any budget hits zero the test is reset and shows "Не сдан".
"Показать ответ" reveals the reference answer without blocking and marks the question with an
orange "Ответ просмотрен" badge. Submitting downloads two files for an external AI agent:
`JavaTestAgent_<dd.mm.yyyy>.md` (grading instructions, template in
`static/prompts/java-test-agent.md`) and `JavaTestResult_<dd.mm.yyyy>.json` (question, user
answer, answer viewed, reference answer, time spent). Nothing is stored server-side.

## HTTP contract

- `GET /` renders the course navigator and an overview.
- `GET /courses/{id}`, `GET /topics/{id}`, `GET /lessons/{id}` render a section page.
- `GET /{courses|topics|lessons}/{id}/test?limit=N` renders the test wizard with N random
  questions of the section (missing or out-of-range `limit` means all).
- `GET /topics/{id}/quiz` redirects to `/topics/{id}` (old URL).
- `GET /api/{courses|topics|lessons}/{id}/quiz?limit=N` returns
  `{ scope, id, title, questions, total }` where each question is
  `{ id, topic, lesson, question, answer }`; questions are shuffled on every request.
  The endpoint is read-only and public.
- Unknown ids, and inactive lessons, return HTTP 404.
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
  `V3__seed_java_core_basics_questions.sql` for Java Core / Основы языка,
  `V4__seed_java_core_internals_questions.sql` for Особенности внутренней работы Java). The migration
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
