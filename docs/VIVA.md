# BugDNA — Viva Q&A & Technical Defense Guide

### 1. Why not just use Sentry?
*"Sentry is a full production observability platform. BugDNA is a focused, self-hosted, privacy-first version we built end to end to demonstrate the engineering internals Sentry hides: deterministic SHA-256 fingerprinting enforced by a MySQL unique key, a click-to-crash session timeline, AI reproduction steps validated against real recorded event IDs, and trigger-driven two-way sync directly with the database."*

### 2. Why 5-second polling instead of WebSockets?
Two reasons:
1. **Direct database visibility:** Changes made directly in the MySQL Command Line Client bypass the Spring Boot application entirely, so an application-layer WebSocket push server would never know a row changed.
2. **Simplicity & statelessness:** MySQL `AFTER INSERT/UPDATE/DELETE` triggers capture every mutation regardless of origin into `activity_log`. A cheap, stateless `GET /api/v1/sync/state` poll (returning 4 scalar values via `setTimeout` chaining + `AbortController`, paused when `document.hidden`) detects every API, Tracker, or CLI change within ≤ 5 seconds with negligible overhead.

### 3. How does the MySQL trigger distinguish API, TRACKER, and MYSQL_CLI changes?
- Every pooled HikariCP connection runs `connection-init-sql: "SET @bugdna_actor = 'API'"`.
- When `BugGroupingService` processes incoming telemetry, it sets `SET @bugdna_actor = 'TRACKER'` and resets it to `'API'` in a `finally` block.
- Flyway's `V3__seed.sql` sets `'SEED'` and resets to `'API'`.
- A teacher typing SQL in the **MySQL Command Line Client** never sets `@bugdna_actor`, so its session variable is `NULL`.
- Each trigger stamps `COALESCE(@bugdna_actor, 'MYSQL_CLI')` into `activity_log.source`.

### 4. Why can't the AI group bugs?
Bug grouping must be **100% deterministic, fast, explainable, and unit-testable**. LLMs are probabilistic and could split identical errors or merge unrelated ones. BugDNA computes a canonical normalized string (versioned `v1|...`) hashed with `SHA-256` and enforces uniqueness at the database level via `UNIQUE(project_id, fingerprint)` plus `SELECT ... FOR UPDATE`. The AI is strictly limited to *explaining* bugs — and our backend drops any AI evidence claim whose cited `eventIds` do not actually exist in the recorded timeline.

### 5. How do you handle race conditions when 20 users hit a brand-new bug simultaneously?
If 20 concurrent requests check for a new fingerprint simultaneously and see nothing, multiple threads attempt `INSERT`. Only one succeeds; the remaining 19 hit `DataIntegrityViolationException` on `UNIQUE(project_id, fingerprint)`. Because JPA marks a failed transaction rollback-only, `BugGroupingService.groupErrorEvent` runs in `@Transactional(propagation = Propagation.REQUIRES_NEW)`. The losing threads catch the constraint exception cleanly and retry in a fresh transaction, acquiring `SELECT ... FOR UPDATE` on the committed bug row and incrementing `occurrence_count`. Our `IngestConcurrencyAndRegressionTest` proves 20 parallel threads produce **exactly 1 bug with `occurrence_count = 20`**.

### 6. Why must the tracker's `fetch` wrapper explicitly check `response.ok`?
Per the MDN Fetch API specification, `fetch()` **does not reject** on HTTP 4xx or 5xx responses (like HTTP 500 `DEMO_COUPON_RULE_ERROR`); it only rejects on network failures (DNS error, offline, CORS block). Without explicitly checking `if (!response.ok)` and cloning the response inside `try/catch` to read `errorCode`, server-side 500 errors would be completely invisible to client error tracking.

### 7. What prevents duplicate events if the browser retries a failed batch?
Every browser event is assigned a client-generated UUID (`eventId = crypto.randomUUID()`). MySQL enforces `UNIQUE KEY uk_event (project_id, event_id)`. When a retried batch arrives, `IngestService` skips already-stored event IDs and returns `duplicates: N`, making ingestion completely **idempotent**.

### 8. What data do you refuse to collect (Privacy / OWASP)?
Following the **OWASP Logging Cheat Sheet**, BugDNA **never** collects form input values, passwords, credit card numbers, cookies, `Authorization` headers, request/response bodies, or query strings. Clicks are captured **only** on elements carrying explicit `data-bugdna-action` attributes (never `innerText`). Both the browser tracker and the Spring Boot server independently mask emails (`[email]`), Bearer/hex tokens (`[token]`), and digit runs ≥ 8 (`[number]`). Prompts sent to the AI additionally strip `sessionId` and `userAgent`.

### 9. Why `ddl-auto=validate` and Flyway instead of `ddl-auto=update`?
Hibernate `ddl-auto=update` cannot manage MySQL triggers, SQL views (`bug_summary`), seed data, or safe schema evolution. Flyway versions every schema change cleanly (`V1`, `V2`, `V3`), and `ddl-auto=validate` guarantees at startup that every Java `@Entity` matches the exact MySQL table definition.

### 10. What would you add next for production?
1. Privacy-masked DOM session replay (e.g., `rrweb`) linked to occurrences.
2. Source-map upload and server-side stack trace symbolication for minified JS bundles.
3. Outbound webhook alerts (Slack / Discord / email) specifically on `REGRESSION` events.
4. OAuth2 / JWT user authentication with multi-project RBAC.

