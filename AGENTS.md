# BugDNA — always-on project rules

Full spec: docs/SPEC.md (the original spec wins on names/requirements). Build plan: docs/ANTIGRAVITY_PROMPT.md (§1–§12). Work ONE phase at a time; finish with the phase's acceptance checks, then stop and wait.

1. Scope = spec + [REFINEMENT] items in ANTIGRAVITY_PROMPT.md. No WebSockets (poll every 5 s), no Kafka/Redis, no ML training, no Spring Security user management in v1 (dev login only).
2. Stack: Java 21 + latest stable Spring Boot (Maven, Web, Data JPA, Validation, MySQL, Flyway + flyway-mysql); React + Vite in JavaScript + Tailwind + React Router; tracker = zero-dependency ES module.
3. DB changes only via Flyway migrations (never edit an applied one). ddl-auto=validate. Triggers populate activity_log; analytics read the bug_summary view.
4. Secrets only in env vars (.env.example with placeholders; .env git-ignored). Never log or expose keys.
5. Privacy: never collect input values, cookies, auth headers, bodies or query strings; mask emails/tokens/long numbers (OWASP Logging Cheat Sheet).
6. Grouping is deterministic (SHA-256 fingerprint + UNIQUE(project_id, fingerprint)); the AI only explains and must cite real event ids.
7. Tracker fetch wrapper must check response.ok (fetch does not reject on HTTP 500); eventId UUIDs make ingest idempotent.
8. UI: invoke the premium-component skill before building a screen; follow .agents/rules/bugdna-app-ui.md > web-interface-guidelines.md > premium-frontend.md; use design-taste-frontend (app-screen section), impeccable, animate; finish with /baseline-ui and /fixing-motion-performance. If a skill step needs a script/reference file that is not in the workspace, say so in one line and follow the SKILL.md text.
9. Every phase ends green: ./mvnw test, npm run build for each frontend, acceptance checks shown in the walkthrough.
10. Keep code explainable in a viva; comment the why in fingerprint, regression, trigger and polling code.
