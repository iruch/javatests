# Java-тесты для Study Hub: project contracts

## Product

"Java-тесты для Study Hub" is a public, unauthenticated learning site. The header links to
"Моя история тестирования" (`/history`) and "Справка по использованию" (`/help`, the rendered
README.md, copied to the classpath as `help/README.md` at build time). A sun/moon button before the
GitHub link switches light/dark theme: colors are CSS variables in `:root` and
`:root[data-theme="dark"]`, the choice is kept in `localStorage` ("javatests-theme"), and until the
user picks one the site follows `prefers-color-scheme`. An inline head script sets the theme before
first paint. The left navigation is a collapsible
tree `course -> topic -> lesson`, each node with its
active question count. Selecting any node opens a section page: linked breadcrumbs
(`Тесты / Java Core / Основы языка / Final`), the total question count of the node, its child
sections and its question bank (ordered, answers collapsible).

Every section has a "Начать тестирование" launcher (10 / 20 / 50 / all random questions). The
test is a client-side wizard (`static/js/test.js`): one question per step, "Назад" / "Далее",
"Сдать тест" on the last step. Each question has its own 2-minute budget that runs only while
the question is open; when any budget hits zero the test is reset and shows "Не сдан".
"Показать ответ" reveals the reference answer without blocking and marks the question with an
orange "Ответ просмотрен" badge.

Every attempt is stored (`POST /api/test-attempts`): a submitted one with its questions, answers and
report files, a timed-out one ("Не сдан") as a summary only. The server renders two report variants,
each a pair of files for an external AI agent: `JavaTestAgent_<dd.mm.yyyy>_<HH-mm-ss>_<full|short>.md`
(grading prompt, templates in `src/main/resources/prompts/`) and
`JavaTestResult_<dd.mm.yyyy>_<HH-mm-ss>_<full|short>.json` (questions, user answers, answer viewed, reference
answers, time spent). "Скачать результат (полный|упрощенный)" downloads both files of a variant. The
history page lists attempts newest first; a row expands to the details and the download buttons.
History is per database (per sandbox), not per user.

## HTTP contract

- `GET /` renders the course navigator and an overview.
- `GET /courses/{id}`, `GET /topics/{id}`, `GET /lessons/{id}` render a section page.
- `GET /{courses|topics|lessons}/{id}/test?limit=N` renders the test wizard with N random
  questions of the section (missing or out-of-range `limit` means all).
- `GET /topics/{id}/quiz` redirects to `/topics/{id}` (old URL).
- `POST /api/test-attempts` stores an attempt `{ scope, scopeId, status: SUBMITTED|FAILED, startedAt,
  finishedAt, timeZone, failedQuestionNumber, answers[{ questionId, userAnswer, answerViewed,
  timeSpentSec }] }` and returns `{ attempt, historyUrl, downloads: { full, short } }`.
- `GET /history` renders the test history; `GET /history/{id}/download/{full|short}/{md|json}`
  returns a stored report file as an attachment.
- `POST /history/{id}/delete` deletes an attempt with its answers and files (after a browser
  confirm) and redirects to `/history`; unknown ids return 404.
- `GET /help` renders README.md.
- The sidebar "Пересобрать приложение" button only confirms and shows how to run
  `scripts/rebuild.{cmd,command,sh}` (a browser cannot run host commands). The scripts confirm again,
  run `docker compose down --volumes --rmi all --remove-orphans` and `docker compose up -d --build`,
  wait for the site, open it and exit; `-DryRun` / `--dry-run` prints the steps only.
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
- Test history: `test_attempts` -> `test_attempt_answers` (question and reference answer are copied,
  so history survives edits of the question bank) and `test_attempt_files` (generated md/json per
  variant, stored as TEXT). Deleting an attempt cascades to its answers and files.
- Runtime data lives in the `postgres_data` Docker volume; `docker compose down -v` erases it.
  Unit tests use an in-memory H2 database (`src/test/resources/application.yml`).

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
