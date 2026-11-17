# JavaTests project contracts

## Product

JavaTests is a public, unauthenticated learning site. The left navigation exposes a
collapsible hierarchy of courses and topics. Selecting a topic opens its quiz in the
main area. Lessons are the future question records and are shown in random order.

## HTTP contract

- `GET /` renders the public course navigator and an empty-state panel.
- `GET /topics/{topicId}/quiz` renders a topic quiz page.
- `GET /api/topics/{topicId}/quiz` returns `{ topic, questions, total }`; questions are
  shuffled on every request. The endpoint is read-only and public.
- Unknown course/topic ids return HTTP 404.
- The application listens on port `8097` by default.

## Data contract

- PostgreSQL is the runtime database. Flyway owns every schema change and seed record.
- The relational hierarchy is `courses -> topics -> lessons`. A lesson may be inactive
  while content is being prepared.
- Course and topic ordering is explicit (`sort_order`); display titles are data, not
  hard-coded in templates.
- Deleting a course cascades to its topics and lessons.

## Delivery contract

- Main development happens on `dev`; merge into `main` only when the user requests it.
- Keep the public site free of authentication and registration flows.
- Prefer server-side Thymeleaf rendering and lightweight dependencies suitable for a
  1 vCPU / 2 GB RAM VPS.
- New schema or seed changes must be a new versioned Flyway migration.
