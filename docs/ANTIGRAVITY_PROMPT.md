# BugDNA — Antigravity Build Prompt (phase by phase)

**Project:** BugDNA, an intelligent bug detection, contextual error tracking and AI-assisted debugging platform
**Course use:** Full-stack project presentation (3-member team)
**Stack:** React (JavaScript, Vite) · Spring Boot (Java 21) · MySQL 8 · REST under `/api/v1` · Gemini or OpenAI API
**Design kit:** your Premium Frontend AI Kit, installed as Antigravity rules and skills (see §0.2)

> How this file is organised:
> §0 tells you how to use it in Antigravity. §1–§7 are the shared context (rules, architecture, folders, schema, API). §8 holds **six self-contained phase prompts**. Paste them one at a time. §9–§12 cover the test cases, demo script, team split and viva answers.

---

## 0. How to use this in Google Antigravity

### 0.1 One-time setup

1. Unzip `bugdna-antigravity-kit.zip` and open the **`bugdna/` folder** as the Antigravity workspace (File → Open Folder). The folder already contains:
   - `AGENTS.md` holds the always-on project rules. Antigravity loads it on every turn.
   - `.agents/rules/` holds the frontend rules from your kit (with the YAML `trigger` frontmatter that Antigravity requires; without it, Antigravity silently ignores a rule file).
   - `.agents/skills/` holds your kit's skills, one folder per skill with a `SKILL.md` file.
   - `docs/ANTIGRAVITY_PROMPT.md` is this file.
   - `docs/SPEC.md` is a placeholder. **Paste your full original BugDNA spec into it.** If the spec and this prompt ever disagree on a name or a requirement, the spec wins, except for the refinements marked **[REFINEMENT]**.
2. Antigravity's current default folder is `.agents/` (plural). It still reads the legacy `.agent/` folder that your kit originally shipped with. If your Antigravity build doesn't list the rules or skills under **… → Customizations**, rename `.agents` to `.agent`.
3. Check **… → Customizations → Rules / Skills**. You should see the rules and skills listed in §0.2.
4. Install locally: JDK 21, Maven (or use the `mvnw` wrapper), Node 20+ (or 22 LTS), and MySQL 8.0/8.4 with the **MySQL Command Line Client**. Create a Gemini API key in Google AI Studio (or an OpenAI key). Keep the key in `backend/.env` and **never** commit it.

### 0.2 Kit files installed in this workspace (real files from `/kit`)

| Installed at | Original kit file | What it's used for |
|---|---|---|
| `.agents/rules/premium-frontend.md` | `premium-frontend.md` (identical to `antigravity/.agent/rules/premium-frontend.md`) | Main frontend rules. Frontmatter added: `trigger: glob` on `dashboard/**, demo-store/**`. Body unchanged. |
| `.agents/rules/references.md` | `references.md` (identical to `antigravity/.agent/rules/references.md`) | Reference list to study before designing. `trigger: model_decision`. |
| `.agents/rules/web-interface-guidelines.md` | `indie-skills/web-interface-guidelines/AGENTS.md` | MUST/SHOULD/NEVER interface checklist. `trigger: glob` on the frontend folders. |
| `.agents/rules/bugdna-app-ui.md` | *(new, written for BugDNA)* | Adapts the premium rules to an **app screen** (a dashboard people work in) instead of a marketing site. It wins when the rules conflict. |
| `.agents/skills/premium-component/SKILL.md` | `antigravity/.agent/workflows/premium-component.md` | Your `/premium-component` workflow migrated to a skill (Antigravity has deprecated workflows). The rules path was updated to `.agents/`. |
| `.agents/skills/impeccable/SKILL.md` | `indie-skills/impeccable/SKILL.md` | Design critique, polish and audit commands. |
| `.agents/skills/design-taste-frontend/SKILL.md` | `indie-skills/taste-skill/SKILL.md` | Anti-slop "taste" rules and the dashboard/app-screen guidance. |
| `.agents/skills/baseline-ui/SKILL.md` | `indie-skills/ui-skills-baseline/SKILL.md` | `/baseline-ui` fast deslop pass. |
| `.agents/skills/fixing-motion-performance/SKILL.md` | `indie-skills/ui-skills-motion-perf/SKILL.md` | `/fixing-motion-performance` audit. |
| `.agents/skills/animate/SKILL.md` + `RECIPES.md` | `indie-skills/emil-kowalski/animate-SKILL.md` + `animate-RECIPES.md` | Emil Kowalski motion rules (renamed `RECIPES.md` because the skill links to that name). |
| `.agents/skills/animation-vocabulary/SKILL.md` | `indie-skills/emil-kowalski/animation-vocabulary-SKILL.md` | Names for motion effects. |

**Vendored-skill caveat:** your kit includes only the `SKILL.md` text of `impeccable` and `taste-skill`. Their `scripts/`, `reference/`/`references/`, `assets/` and `library/` folders are **not** in the kit. Tell the agent: *"If a skill step calls a script or reference file that doesn't exist, say so in one line, follow the SKILL.md text instead, and keep going."*

**Kit files intentionally not installed:** `USER_RULES_paste.txt` (a global marketing-site rule that would push WebGL, Lenis and custom cursors into a dashboard), `cursor/.cursor/rules/*.mdc` (Cursor-only copies of the same rules), `optional/vendor-company-skills/*` (GSAP and three.js aren't needed for this app; the kit README also marks them "not installed by default"), and `src/*` (upstream READMEs).

### 0.3 How to run the phases

1. Use **Planning mode**. Pick the strongest available model for Phases 1, 4 and 5. Any model works for the rest.
2. Paste **one** phase block from §8. The agent should produce an *Implementation Plan* artifact. Read it, then approve.
3. After the agent builds, it must run the **Acceptance checks** for that phase and write a short *Walkthrough* with commands, screenshots and results.
4. **Don't start the next phase until every check passes.** If a check fails, reply with `Fix: <check id> — <what you saw>`.
5. Commit after every phase: `git commit -m "phase N: <name>"`. Tag the working demo state: `git tag demo-ready`.
6. Allow terminal commands, but deny any command that drops your real databases or touches files outside the workspace.

---

## 1. Global rules (these also live in `AGENTS.md`, always on)

1. **Scope is the spec plus the refinements below.** No extra frameworks, no WebSockets (we poll), no Kafka or Redis, no custom ML training, no Spring Security user management in v1.
2. **Language and versions:** backend uses Java 21 and the latest stable Spring Boot from start.spring.io, with Maven, Spring Web, Spring Data JPA, Validation, MySQL driver and Flyway (`flyway-mysql`; on Boot 4.x also add `spring-boot-starter-flyway`). Frontends use React 18+/19 in **JavaScript** with Vite, Tailwind CSS and React Router. The tracker is plain ES-module JavaScript with zero dependencies.
3. **Database changes go only through Flyway migrations** in `backend/src/main/resources/db/migration`. Never edit an applied migration; add a new `V<n>__*.sql` instead. Set `spring.jpa.hibernate.ddl-auto=validate`.
4. **Secrets live only in env vars:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `AI_PROVIDER`, `GEMINI_API_KEY`, `OPENAI_API_KEY`, `AI_MODEL`, `BUGDNA_DEV_USER`, `BUGDNA_DEV_PASSWORD`, `BUGDNA_CORS_ORIGINS`, `VITE_API_BASE`, `VITE_BUGDNA_PROJECT_KEY`. Provide a `.env.example` for each app, and keep `.env` in `.gitignore`. Never log keys, and never send them to the browser.
5. **Privacy (OWASP Logging Cheat Sheet):** never collect input values, passwords, card numbers, cookies, auth headers, request or response bodies, or full query strings. Mask emails, tokens and long digit runs before storing anything and before sending anything to the AI.
6. **Deterministic over clever:** grouping uses the SHA-256 fingerprint and the DB unique key, never the AI. The AI only explains.
7. **Every phase ends green:** `./mvnw test` passes, `npm run build` passes for each frontend, and the acceptance checks are demonstrated.
8. **UI work** follows `.agents/rules/bugdna-app-ui.md`, then `premium-frontend.md`, then `web-interface-guidelines.md`. Before building any screen, invoke the `premium-component` skill. Use `design-taste-frontend` (app-screen section) and `impeccable` for the design pass, `animate` for motion, and finish with `/baseline-ui` and `/fixing-motion-performance` on the changed files.
9. Prefer small, readable code that a student can explain in a viva. Comment the *why* in the fingerprint, regression, trigger and polling code.

---

## 2. Architecture

```
┌──────────────────────┐   tracked events (batch, X-BugDNA-Key)   ┌──────────────────────────────┐
│ Demo Store (React)   │ ───────────────────────────────────────▶ │ Spring Boot  :8080            │
│ :5174  + tracker.js  │ ◀── /demo-api/* (store's own backend) ──  │  /api/v1/ingest  → Ingest     │
└──────────────────────┘                                           │   ├ sanitize → dedupe eventId │
                                                                   │   ├ fingerprint (SHA-256)     │
┌──────────────────────┐   REST /api/v1 (Bearer dev token)         │   ├ upsert bug + occurrence   │
│ Dashboard (React)    │ ◀──────────────────────────────────────▶  │   └ regression check          │
│ :5173  polls 5 s     │                                           │  /api/v1/bugs, comments,      │
└──────────────────────┘                                           │   activity, sync, analytics   │
                                                                   │  AI service → Gemini/OpenAI   │
┌──────────────────────┐   SQL (UPDATE/INSERT/DELETE)              └──────────────┬───────────────┘
│ MySQL Command Line   │ ─────────────────────────────────────────▶ MySQL 8 ──────┘
│ Client (teacher)     │    triggers write activity_log; view bug_summary feeds analytics
└──────────────────────┘
```

**Two-way sync loop**
- **Dashboard → MySQL:** a PATCH or POST goes through JPA to MySQL, and a trigger writes an `activity_log` row with `source='API'`.
- **MySQL CLI → Dashboard:** an UPDATE, INSERT or DELETE fires a trigger that writes an `activity_log` row with `source='MYSQL_CLI'`. On the next poll (≤ 5 s), `GET /api/v1/sync/state` sees the new `maxActivityId` or `bugsMaxUpdatedAt`, and the dashboard refetches the affected data and shows a feed entry.
- **How the trigger tells API from CLI:** HikariCP runs `SET @bugdna_actor = 'API'` on every pooled connection (`connection-init-sql`). The ingest transaction sets `'TRACKER'`. A CLI session has no variable set, so `COALESCE(@bugdna_actor,'MYSQL_CLI')` labels it.

---

## 3. Folder structure

```
bugdna/
├─ AGENTS.md
├─ .agents/ (rules/, skills/)
├─ docs/  ANTIGRAVITY_PROMPT.md · SPEC.md · DEMO_SCRIPT.md · VIVA.md · ER.md
├─ database/
│  ├─ setup.sql            # create DB + app user + trigger permission (run once as root)
│  └─ demo_queries.sql     # teacher demo for MySQL Command Line Client
├─ backend/                # Spring Boot, Java 21, Maven
│  ├─ .env.example
│  └─ src/main/java/com/bugdna/
│     ├─ config/   (CorsConfig, AuthInterceptor, WebConfig, AiConfig)
│     ├─ auth/     (AuthController, DevTokenStore)
│     ├─ ingest/   (IngestController, IngestService, Sanitizer, FingerprintService, dto/)
│     ├─ bug/      (Bug, BugRepository, BugService, BugController, BugOccurrence…)
│     ├─ event/    (Event, TrackingSession, repositories, TimelineService)
│     ├─ comment/  (Comment, CommentRepository, CommentController)
│     ├─ activity/ (ActivityLog, ActivityController, SyncController)
│     ├─ analytics/(BugSummaryView, AnalyticsController)
│     ├─ ai/       (AiProvider, GeminiProvider, OpenAiProvider, MockAiProvider, AiAnalysisService, AiAnalysis)
│     ├─ demo/     (DemoStoreController  → /demo-api/**)
│     └─ common/   (ApiExceptionHandler → ProblemDetail)
│     resources/ application.yml · db/migration/V1__schema.sql V2__triggers_and_view.sql V3__seed.sql
├─ dashboard/              # React + Vite, :5173
│  └─ src/ api/ hooks/(usePolling, useSyncState) components/ pages/(Login, Bugs, BugDetail, Analytics) styles/tokens.css
├─ demo-store/             # React + Vite, :5174 (intentionally buggy shop)
│  └─ src/ pages/(Home, Product, Cart, Checkout) bugdna.js (tracker init)
├─ tracker/
│  ├─ bugdna-tracker.js    # reusable monitoring module (ESM, no deps)
│  └─ README.md
└─ docker-compose.yml      # optional MySQL 8.4 if no local install
```

---

## 4. Database schema (Flyway)

**Tables:** the 7 core tables are `projects`, `tracking_sessions`, `events`, `bugs`, `bug_occurrences`, `comments` and `ai_analyses`. **[REFINEMENT]** adds `activity_log`. The spec's `users` table is **optional in v1** because login is a dev login (see §5). If you keep it, add it in a later migration and make `assignee` reference it.

> If your original spec uses different table or column names, keep the spec's names and adjust the triggers and view to match. The teacher demo relies only on `bugs(id, title, status)` and `comments`.

### `database/setup.sql` (run once as root in MySQL CLI)

```sql
CREATE DATABASE IF NOT EXISTS bugdna CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'bugdna'@'localhost' IDENTIFIED BY 'CHANGE_ME_LOCAL_ONLY';
GRANT ALL PRIVILEGES ON bugdna.* TO 'bugdna'@'localhost';
-- MySQL 8 has binary logging on by default; without this, a non-root user gets
-- ERROR 1419 when Flyway creates triggers.
SET PERSIST log_bin_trust_function_creators = 1;
FLUSH PRIVILEGES;
```

### `V1__schema.sql`

```sql
CREATE TABLE projects (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  name           VARCHAR(120) NOT NULL,
  project_key    VARCHAR(64)  NOT NULL UNIQUE,         -- sent by tracker in X-BugDNA-Key
  allowed_origin VARCHAR(255) NULL,
  created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE tracking_sessions (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id    BIGINT NOT NULL,
  session_id    VARCHAR(64) NOT NULL,                  -- random id from tracker, not a user id
  user_agent    VARCHAR(255) NULL,
  first_seen_at DATETIME(3) NOT NULL,
  last_seen_at  DATETIME(3) NOT NULL,
  UNIQUE KEY uk_session (project_id, session_id),
  CONSTRAINT fk_sess_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE bugs (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id       BIGINT NOT NULL,
  fingerprint      CHAR(64) NOT NULL,                  -- SHA-256 hex
  title            VARCHAR(200) NOT NULL,
  error_type       VARCHAR(40)  NOT NULL,              -- JS_ERROR | PROMISE_REJECTION | API_ERROR | NETWORK_ERROR
  error_code       VARCHAR(80)  NULL,
  http_method      VARCHAR(10)  NULL,
  endpoint         VARCHAR(255) NULL,
  page_path        VARCHAR(255) NULL,
  status           ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED','IGNORED') NOT NULL DEFAULT 'OPEN',
  priority         ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  assignee         VARCHAR(80)  NULL,
  occurrence_count INT NOT NULL DEFAULT 1,
  is_regression    TINYINT(1) NOT NULL DEFAULT 0,
  regression_count INT NOT NULL DEFAULT 0,
  first_seen_at    DATETIME(3) NOT NULL,
  last_seen_at     DATETIME(3) NOT NULL,
  resolved_at      DATETIME(3) NULL,
  created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_bug_fingerprint (project_id, fingerprint),   -- the grouping guarantee
  KEY idx_bugs_status (status),
  KEY idx_bugs_updated (updated_at),
  CONSTRAINT fk_bug_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE events (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id   BIGINT NOT NULL,
  session_pk   BIGINT NOT NULL,
  event_id     VARCHAR(64) NOT NULL,                   -- client UUID, used for idempotency
  type         ENUM('PAGE_VIEW','ROUTE_CHANGE','CLICK','JS_ERROR','PROMISE_REJECTION','API_ERROR','NETWORK_ERROR','CUSTOM') NOT NULL,
  action_label VARCHAR(80)  NULL,                      -- from data-bugdna-action only
  page_path    VARCHAR(255) NULL,                      -- no query string
  http_method  VARCHAR(10)  NULL,
  http_status  SMALLINT     NULL,
  endpoint     VARCHAR(255) NULL,                      -- normalized, no query string
  error_code   VARCHAR(80)  NULL,
  message      VARCHAR(500) NULL,                      -- sanitized
  stack_top    VARCHAR(500) NULL,                      -- first frames, sanitized
  occurred_at  DATETIME(3) NOT NULL,
  received_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  bug_id       BIGINT NULL,
  UNIQUE KEY uk_event (project_id, event_id),          -- duplicate batches are ignored
  KEY idx_events_session_time (session_pk, occurred_at),
  KEY idx_events_bug (bug_id),
  CONSTRAINT fk_evt_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_evt_session FOREIGN KEY (session_pk) REFERENCES tracking_sessions(id),
  CONSTRAINT fk_evt_bug     FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE SET NULL
);

CREATE TABLE bug_occurrences (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id           BIGINT NOT NULL,
  session_pk       BIGINT NOT NULL,
  trigger_event_pk BIGINT NOT NULL,
  occurred_at      DATETIME(3) NOT NULL,
  was_regression   TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_occ_event (trigger_event_pk),
  KEY idx_occ_bug_time (bug_id, occurred_at),
  CONSTRAINT fk_occ_bug     FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE,
  CONSTRAINT fk_occ_session FOREIGN KEY (session_pk) REFERENCES tracking_sessions(id),
  CONSTRAINT fk_occ_event   FOREIGN KEY (trigger_event_pk) REFERENCES events(id) ON DELETE CASCADE
);

CREATE TABLE comments (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id     BIGINT NOT NULL,
  author     VARCHAR(80) NOT NULL,
  body       TEXT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_comments_bug (bug_id),
  CONSTRAINT fk_comment_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE
);

CREATE TABLE ai_analyses (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  bug_id          BIGINT NOT NULL,
  provider        VARCHAR(20) NOT NULL,               -- gemini | openai | mock
  model           VARCHAR(80) NOT NULL,
  status          ENUM('OK','FAILED','MOCK') NOT NULL,
  summary         TEXT NULL,
  evidence_json   JSON NULL,                          -- [{statement, eventIds[]}] — facts only
  hypotheses_json JSON NULL,                          -- [{cause, confidence, reasoning}] — guesses, labeled
  debug_steps_json JSON NULL,
  repro_steps_json JSON NULL,
  input_hash      CHAR(64) NOT NULL,                  -- cache key of the sanitized prompt input
  error_message   VARCHAR(500) NULL,
  created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_ai_bug (bug_id, created_at),
  CONSTRAINT fk_ai_bug FOREIGN KEY (bug_id) REFERENCES bugs(id) ON DELETE CASCADE
);

-- [REFINEMENT] live activity feed; no FKs so deleted rows keep their history
CREATE TABLE activity_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_type ENUM('BUG','COMMENT') NOT NULL,
  entity_id   BIGINT NOT NULL,
  bug_id      BIGINT NULL,
  action      ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  summary     VARCHAR(255) NOT NULL,
  old_values  JSON NULL,
  new_values  JSON NULL,
  source      VARCHAR(20) NOT NULL,                   -- API | TRACKER | MYSQL_CLI
  created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_activity_created (created_at)
);
```

### `V2__triggers_and_view.sql` **[REFINEMENT]**

Each trigger body is a single statement, so Flyway needs no `DELIMITER`. Cascade deletes don't fire child-table triggers in MySQL, so deleting a bug logs one BUG/DELETE row and no per-comment rows. That's expected.

```sql
-- ===== bugs =====
CREATE TRIGGER trg_bugs_ai AFTER INSERT ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, new_values, source)
  VALUES ('BUG', NEW.id, NEW.id, 'INSERT',
          CONCAT('Bug #', NEW.id, ' created: ', LEFT(NEW.title, 150)),
          JSON_OBJECT('status', NEW.status, 'priority', NEW.priority, 'title', NEW.title),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- logs only meaningful changes (status/priority/assignee/title/regression), not every occurrence bump
CREATE TRIGGER trg_bugs_au AFTER UPDATE ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, new_values, source)
  SELECT 'BUG', NEW.id, NEW.id, 'UPDATE',
         LEFT(CONCAT('Bug #', NEW.id, ': ', CONCAT_WS('; ',
           IF(OLD.status   <=> NEW.status,   NULL, CONCAT('status ', OLD.status, ' -> ', NEW.status)),
           IF(OLD.priority <=> NEW.priority, NULL, CONCAT('priority ', OLD.priority, ' -> ', NEW.priority)),
           IF(OLD.assignee <=> NEW.assignee, NULL, CONCAT('assignee ', IFNULL(OLD.assignee,'none'), ' -> ', IFNULL(NEW.assignee,'none'))),
           IF(OLD.title    <=> NEW.title,    NULL, 'title edited'),
           IF(OLD.is_regression <=> NEW.is_regression OR NEW.is_regression = 0, NULL, 'REGRESSION detected'))), 255),
         JSON_OBJECT('status', OLD.status, 'priority', OLD.priority, 'assignee', OLD.assignee, 'title', OLD.title),
         JSON_OBJECT('status', NEW.status, 'priority', NEW.priority, 'assignee', NEW.assignee, 'title', NEW.title),
         COALESCE(@bugdna_actor, 'MYSQL_CLI')
  FROM DUAL
  WHERE NOT (OLD.status <=> NEW.status AND OLD.priority <=> NEW.priority
             AND OLD.assignee <=> NEW.assignee AND OLD.title <=> NEW.title
             AND OLD.is_regression <=> NEW.is_regression);

CREATE TRIGGER trg_bugs_ad AFTER DELETE ON bugs FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, source)
  VALUES ('BUG', OLD.id, OLD.id, 'DELETE',
          CONCAT('Bug #', OLD.id, ' deleted: ', LEFT(OLD.title, 150)),
          JSON_OBJECT('status', OLD.status, 'title', OLD.title),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- ===== comments =====
CREATE TRIGGER trg_comments_ai AFTER INSERT ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, new_values, source)
  VALUES ('COMMENT', NEW.id, NEW.bug_id, 'INSERT',
          CONCAT(NEW.author, ' commented on bug #', NEW.bug_id, ': ', LEFT(NEW.body, 120)),
          JSON_OBJECT('author', NEW.author, 'body', LEFT(NEW.body, 500)),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

CREATE TRIGGER trg_comments_au AFTER UPDATE ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, new_values, source)
  SELECT 'COMMENT', NEW.id, NEW.bug_id, 'UPDATE',
         CONCAT('Comment #', NEW.id, ' on bug #', NEW.bug_id, ' edited'),
         JSON_OBJECT('body', LEFT(OLD.body, 500)), JSON_OBJECT('body', LEFT(NEW.body, 500)),
         COALESCE(@bugdna_actor, 'MYSQL_CLI')
  FROM DUAL WHERE NOT (OLD.body <=> NEW.body AND OLD.author <=> NEW.author);

CREATE TRIGGER trg_comments_ad AFTER DELETE ON comments FOR EACH ROW
  INSERT INTO activity_log (entity_type, entity_id, bug_id, action, summary, old_values, source)
  VALUES ('COMMENT', OLD.id, OLD.bug_id, 'DELETE',
          CONCAT('Comment #', OLD.id, ' on bug #', OLD.bug_id, ' deleted'),
          JSON_OBJECT('author', OLD.author, 'body', LEFT(OLD.body, 500)),
          COALESCE(@bugdna_actor, 'MYSQL_CLI'));

-- ===== analytics view =====
CREATE OR REPLACE VIEW bug_summary AS
SELECT b.id, b.project_id, b.title, b.error_type, b.status, b.priority, b.assignee,
       b.occurrence_count, b.is_regression, b.regression_count,
       b.first_seen_at, b.last_seen_at, b.resolved_at,
       COUNT(DISTINCT o.session_pk) AS sessions_affected,
       COUNT(DISTINCT c.id)         AS comment_count,
       TIMESTAMPDIFF(MINUTE, b.first_seen_at, COALESCE(b.resolved_at, NOW(3))) AS minutes_open
FROM bugs b
LEFT JOIN bug_occurrences o ON o.bug_id = b.id
LEFT JOIN comments c        ON c.bug_id = b.id
GROUP BY b.id;
```

### `V3__seed.sql`

- One project: `Demo Store`, `project_key='demo-store-dev-key'` (a local dev value; the store reads it from `VITE_BUGDNA_PROJECT_KEY`).
- Bugs with explicit ids **101–106**, so `WHERE id = 104` works in the teacher demo. Seed realistic, varied rows: a JS TypeError on `/product/:id`, a 404 on `GET /demo-api/products/:id`, a NETWORK_ERROR, one RESOLVED, one IN_PROGRESS with an assignee, and one flagged regression. **Don't** seed the SAVE20 bug; it must be created live in the demo.
- Seed fingerprints with `SHA2('<canonical string>', 256)`, using **exactly** the canonical format from §6.2. A unit test must prove Java produces the same hash for bug 104's canonical string.
- Add 2 tracking sessions, 15–25 events forming realistic timelines for 3 of the seeded bugs, 4–6 comments, and occurrences.
- End with `ALTER TABLE bugs AUTO_INCREMENT = 200;`
- Flyway runs on the app's connection pool, so seed rows would otherwise be labeled `API` in `activity_log`. Start V3 with `SET @bugdna_actor = 'SEED';` and end it with `SET @bugdna_actor = 'API';` so the connection goes back to the pool correctly labeled.

### `database/demo_queries.sql` **[REFINEMENT]** (teacher runs these in the MySQL Command Line Client)

```sql
USE bugdna;

-- 1) MySQL -> Dashboard: change a status; the dashboard updates within 5 s and the feed shows "MYSQL_CLI"
UPDATE bugs SET status = 'RESOLVED' WHERE id = 104;
SELECT id, title, status, updated_at FROM bugs WHERE id = 104;

-- 2) Dashboard -> MySQL: after changing priority/assignee in the UI, verify here
SELECT id, title, status, priority, assignee, updated_at FROM bugs ORDER BY updated_at DESC LIMIT 5;

-- 3) Insert a comment from the CLI; it appears under bug 104 and in the live feed
INSERT INTO comments (bug_id, author, body) VALUES (104, 'Teacher (CLI)', 'Checked from MySQL CLI - please verify the fix.');
SELECT id, bug_id, author, body, created_at FROM comments WHERE bug_id = 104 ORDER BY id DESC LIMIT 3;

-- 4) Delete that comment from the CLI; it disappears and the feed logs the delete
DELETE FROM comments WHERE bug_id = 104 AND author = 'Teacher (CLI)';

-- 5) Reassign / reprioritise from the CLI
UPDATE bugs SET priority = 'CRITICAL', assignee = 'Member 2' WHERE id = 104;

-- 6) Automatic app -> MySQL: after triggering SAVE20 in the demo store
SELECT id, title, error_code, occurrence_count, status, is_regression FROM bugs ORDER BY id DESC LIMIT 3;
SELECT e.id, e.event_id, e.type, e.action_label, e.page_path, e.http_status, e.error_code, e.occurred_at
FROM events e ORDER BY e.id DESC LIMIT 10;

-- 7) Prove grouping: the unique key forbids a second row with the same fingerprint
SELECT project_id, fingerprint, COUNT(*) FROM bugs GROUP BY project_id, fingerprint HAVING COUNT(*) > 1;  -- returns 0 rows

-- 8) The live feed and the analytics view
SELECT id, entity_type, action, source, summary, created_at FROM activity_log ORDER BY id DESC LIMIT 10;
SELECT id, title, status, occurrence_count, sessions_affected, comment_count, minutes_open FROM bug_summary ORDER BY occurrence_count DESC;

-- 9) Show the triggers and the view exist
SHOW TRIGGERS FROM bugdna;
SHOW CREATE VIEW bug_summary\G

-- 10) Delete a whole bug from the CLI (comments/occurrences cascade); it disappears from the list
-- DELETE FROM bugs WHERE id = 106;
```

---

## 5. API contract (`/api/v1`, JSON, errors as RFC 9457 `ProblemDetail`)

**Auth [REFINEMENT: simplified v1].** Use a dev login, not Spring Security user management. `POST /auth/login {username, password}` checks against `BUGDNA_DEV_USER`/`BUGDNA_DEV_PASSWORD` and returns `{token, displayName}`. The token is a random UUID held in an in-memory map with an 8 h TTL. A `HandlerInterceptor` requires `Authorization: Bearer <token>` on `/api/v1/**`, except `/auth/login`, `/ingest/**` and `/health`. Also `POST /auth/logout` and `GET /auth/me`. The dashboard keeps the token in `sessionStorage`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/ingest/events` | Header `X-BugDNA-Key`. Body `{sessionId, userAgent?, events:[…≤50]}`. Returns `202 {accepted, duplicates, bugsCreated:[id], bugsUpdated:[id], regressions:[id]}`. A wrong key returns 401. |
| GET | `/bugs` | Filters `status, priority, errorType, q, sort=lastSeen|occurrences|priority`. Returns list rows (id, title, status, priority, assignee, occurrenceCount, isRegression, lastSeenAt, fingerprint short). |
| GET | `/bugs/{id}` | Detail, including `sessionsAffected` (from `bug_summary`) and `fingerprint`. |
| PATCH | `/bugs/{id}` | `{status?, priority?, assignee?, title?}`. Validates enums. RESOLVED sets `resolved_at`. Moving away from RESOLVED/CLOSED clears it. |
| DELETE | `/bugs/{id}` | Hard delete (the UI confirms with an AlertDialog). |
| GET | `/bugs/{id}/occurrences` | Newest first, with session id and `wasRegression`. |
| GET | `/bugs/{id}/timeline?occurrenceId=` | For one occurrence (default: latest), returns events from the **same session** in the window `[trigger − 120 s, trigger + 10 s]`, max 30, ordered ascending, with the trigger event flagged. |
| GET / POST | `/bugs/{id}/comments` | List, or add `{author, body}` (body 1–2000 chars). |
| PATCH / DELETE | `/comments/{id}` | Edit or delete a comment. |
| POST | `/bugs/{id}/analysis?force=false` | Runs AI analysis (cached by `input_hash` unless `force`). |
| GET | `/bugs/{id}/analysis` | Latest analysis or `204`. |
| GET | `/activity?afterId=&limit=50` | Feed rows, newest first. `afterId` returns only newer rows. |
| GET | `/sync/state` | `{maxActivityId, bugsMaxUpdatedAt, bugCount, commentCount, serverTime}`. Cheap, and polled every 5 s. |
| GET | `/analytics/overview` | From `bug_summary`: counts by status and priority, top 5 by occurrences, regression count, mean minutes-to-resolve, plus errors per day for the last 14 days (from `events`). |
| GET | `/projects` | List projects (key masked). |
| GET | `/health` | `{status:"UP"}`. |

**Demo store backend (not part of the BugDNA API):** `GET /demo-api/products`, `GET /demo-api/products/{id}` (an unknown id returns 404), `POST /demo-api/cart/coupon {code}` (accepts `SAVE20` with 200, so the bug hides until checkout), and `POST /demo-api/checkout {items, coupon}`. **If coupon is `SAVE20`, checkout returns HTTP 500 `{"errorCode":"DEMO_COUPON_RULE_ERROR","message":"Coupon rule evaluation failed"}`.**

**CORS:** allow only the origins in `BUGDNA_CORS_ORIGINS` (default `http://localhost:5173,http://localhost:5174`).

---

## 6. Core algorithms (the viva material)

### 6.1 Tracker (`tracker/bugdna-tracker.js`)
- `BugDNA.init({ endpoint, projectKey, maxBuffer: 30, flushIntervalMs: 10000 })`. `sessionId` = `crypto.randomUUID()` stored in `sessionStorage`. Every event gets its own `eventId = crypto.randomUUID()`.
- **Captures:** `PAGE_VIEW` and `ROUTE_CHANGE` (patch `history.pushState`/`replaceState` and listen to `popstate`); `CLICK` **only** on elements with `data-bugdna-action="APPLY_COUPON"` (the label comes from the attribute, never `innerText` or input values); `JS_ERROR` (`window.addEventListener('error')`, using `error.name`, message and the first 3 stack frames); `PROMISE_REJECTION` (`unhandledrejection`); `API_ERROR` and `NETWORK_ERROR` through a `fetch` wrapper.
- **[REFINEMENT] The `fetch` wrapper must check `response.ok`.** `fetch` does **not** reject on HTTP 4xx/5xx; it only rejects on network failure. So: `const res = await origFetch(...); if (!res.ok) { record API_ERROR with method, normalized endpoint, status, and errorCode read from res.clone().json().errorCode inside try/catch; } return res;`. In `catch`, record `NETWORK_ERROR` and **rethrow**. Never track requests to the BugDNA endpoint itself (no feedback loops).
- **Sanitize in the browser:** strip query strings and hashes, replace numeric or UUID path segments with `:id`, mask emails (`[email]`), `Bearer …`/long tokens (`[token]`) and digit runs ≥ 8 (`[number]`), and truncate messages to 300 characters.
- **Sending:** a ring buffer holds the last 30 events. On an error event, flush immediately; otherwise flush every 10 s and on `visibilitychange→hidden`. Send with `fetch(..., {keepalive:true})` and header `X-BugDNA-Key`. On failure, keep the batch in `sessionStorage` and retry with backoff (2 s, 4 s, 8 s, max 3). **[REFINEMENT] Resends are safe** because the server dedupes on `(project_id, event_id)`.

### 6.2 Fingerprint (`FingerprintService`, SHA-256)
Canonical string (version prefix so the algorithm can change later):
- API errors: `v1|API_ERROR|{METHOD}|{normalizedEndpoint}|{status}|{errorCode or "-"}`
- JS errors and rejections: `v1|{type}|{errorName}|{normalizedMessage}|{topFrame file:function, no line/col}`
- Network errors: `v1|NETWORK_ERROR|{METHOD}|{normalizedEndpoint}`

Normalization: lowercase the endpoint, replace numbers/UUIDs/hex ids with `<id>`, quoted literals with `<s>` and other numbers with `<n>`, and collapse whitespace. `fingerprint = hex(SHA-256(utf8(canonical)))`. Grouping relies on **`UNIQUE (project_id, fingerprint)`**: two concurrent inserts can't create duplicates.

### 6.3 Ingest pipeline (`IngestService`, one `@Transactional` per batch)
1. Resolve the project from `X-BugDNA-Key` (401 if unknown). Run `SET @bugdna_actor='TRACKER'` on this connection, and reset it to `'API'` in `finally`.
2. Upsert `tracking_sessions` (update `last_seen_at`).
3. For each event, sanitize again server-side (never trust the client), then insert. **[REFINEMENT] A duplicate `(project_id,event_id)` counts as `duplicates++` and is skipped**: use `INSERT IGNORE`, or check first and catch `DataIntegrityViolationException`.
4. For each new error event: compute the fingerprint and `SELECT … FOR UPDATE` the bug by `(project_id, fingerprint)`.
   - **None:** insert the bug (title such as `POST /api/orders/checkout → 500 DEMO_COUPON_RULE_ERROR` or `TypeError: Cannot read properties of undefined (reading 'price')`). If the unique key races, catch the exception and re-select.
   - **Exists:** `occurrence_count+1`, `last_seen_at=now`.
   - **[REFINEMENT] Regression:** if the existing status is `RESOLVED` or `CLOSED`, set `status='REOPENED'`, `is_regression=1`, `regression_count+1`, `resolved_at=NULL`, and mark the occurrence `was_regression=1`. `IGNORED` bugs stay ignored, but their count still rises.
   - Insert a `bug_occurrences` row and set `events.bug_id`.
5. Return the summary.

### 6.4 AI analysis (`AiAnalysisService`, evidence vs hypothesis)
- `AiProvider` interface with `GeminiProvider` (REST `generateContent`, key in header `x-goog-api-key` from `GEMINI_API_KEY`), `OpenAiProvider` (`OPENAI_API_KEY`) and `MockAiProvider`. `AI_PROVIDER=gemini|openai|mock`. `AI_MODEL` comes from env, so pick a current fast-tier model from the provider's model list; don't hardcode a model name. With no key, fall back to mock so the demo never breaks.
- **Input (sanitized only):** bug fields, fingerprint canonical string, occurrence count, sessions affected, regression flag, and the latest timeline (`eventId, type, actionLabel, pagePath, method, endpoint, status, errorCode, message, stackTop`). No session ids, user agents or raw bodies.
- **Output:** strict JSON via a response schema: `{summary, evidence:[{statement, eventIds:[…]}], hypotheses:[{cause, confidence:"low|medium|high", reasoning}], debuggingSteps:[…], reproductionSteps:[…]}`.
- **Validation:** every `evidence.eventIds` value must exist in the input timeline, or the item is dropped. Drop hypotheses with no reasoning. Store the row with `input_hash = SHA-256(input JSON)` and reuse the cache when it matches.
- **UI:** "Evidence (from recorded events)" shows chips that jump to the timeline event. "Hypotheses (AI guesses, verify before acting)" shows confidence badges. Always include a disclaimer.

### 6.5 Two-way sync (dashboard `useSyncState` hook)
- Poll `GET /sync/state` every **5 s** with `setTimeout` chaining (not `setInterval`), using `AbortController`. Pause while `document.hidden` and refresh immediately on focus.
- If `maxActivityId` changed, fetch `/activity?afterId=` and prepend to the feed. Then, if `bugsMaxUpdatedAt`, `bugCount` or `commentCount` changed, refetch the bug list, plus the open bug's detail and comments.
- Highlight changed rows for 1.2 s (opacity/transform only; reduced-motion uses a static marker). Show "Synced 3 s ago" with a status dot. If a polled change hits a bug that's being edited, show the server value and a "changed in MySQL" note; never silently overwrite the user's open edit.

---

## 7. UI direction (from the kit, adapted to an app screen)

- **Mode:** this is an *app screen* ("Operate" mode in `impeccable`, "app screen" in `design-taste-frontend`). Ambition shows as precision and restraint. **No** WebGL, Lenis, custom cursor or magnetic buttons in the dashboard (`bugdna-app-ui.md` overrides those parts of `premium-frontend.md`).
- **Concept (one idea, §1 of premium-frontend):** *every bug has a genome.* The SHA-256 fingerprint is drawn as a 64-cell "DNA strand" (one cell per hex char, mapped to 4 tones). It appears in the bug header and as a thin strip on each row. **Signature moment:** when a new occurrence arrives, that strand pulses once. A regression shows a red "mutation" tick and a `REGRESSION` tag.
- **Dark-first palette (one dominant, one neutral, one sharp accent):** bg `#0B0C0E`, panel `#121418`, line `#22262C`, text `#ECEAE4`, muted `#8B9097`, accent `#C8F169` (signal lime, for live and sync only), danger `#FF5D5D`, warn `#F2B544`. Use no purple or blue gradients and no glass.
- **Type:** Geist for UI and Geist Mono for ids, hashes, endpoints and timestamps (11–12 px uppercase labels, +0.05em tracking). Never use Inter, Roboto or Poppins.
- **Layout:** a 12-column grid. The bug list is a dense table (not cards) with a right-hand **Live Activity** rail. Bug detail uses a split layout: timeline on the left (vertical, mono timestamps, relative "−8.2 s" offsets, trigger event emphasized) and status controls, AI panel and comments on the right.
- **Motion (animate skill tokens):** feedback ≤ 200 ms; feed items enter with opacity and translateY over 220 ms ease-out; no animation on keyboard-repeated actions; `prefers-reduced-motion` disables all non-essential motion.
- **Craft:** every control has hover, active and focus-visible states. Use Radix or Base UI primitives for selects, dialogs and the AlertDialog on delete. Use skeleton loading states, errors next to the action, URL-synced filters (`?status=OPEN`), AA contrast, and `h-dvh` instead of `h-screen`.
- **Copy:** real and specific ("3 sessions hit this in the last hour"), never lorem.

---

## 8. Phase prompts (paste one at a time)

### PHASE 1 — Database, migrations, backend CRUD, dev login

```text
You are building BugDNA (see docs/ANTIGRAVITY_PROMPT.md §1–§6 and docs/SPEC.md). Follow AGENTS.md.
PHASE 1 ONLY: database + Spring Boot foundation. Do not build any frontend yet.

Tasks
1. Create database/setup.sql and database/demo_queries.sql exactly as in §4.
2. Generate backend/ (Java 21, Maven wrapper, latest stable Spring Boot): Web, Data JPA, Validation,
   MySQL driver, Flyway + flyway-mysql (Boot 4.x: also spring-boot-starter-flyway). application.yml reads
   DB_URL/DB_USER/DB_PASSWORD, ddl-auto=validate, and
   spring.datasource.hikari.connection-init-sql: "SET @bugdna_actor = 'API'".
   Add backend/.env.example (no real values) and .gitignore.
3. Flyway migrations V1__schema.sql, V2__triggers_and_view.sql, V3__seed.sql per §4 (seed bug ids 101–106,
   fingerprints via SHA2 of the §6.2 canonical strings, AUTO_INCREMENT 200).
4. JPA entities + repositories for all tables; a read-only @Immutable entity for the bug_summary view.
5. REST: /api/v1/auth/login|logout|me (dev login from BUGDNA_DEV_USER/BUGDNA_DEV_PASSWORD, in-memory token,
   HandlerInterceptor — no Spring Security), /bugs (list/detail/PATCH/DELETE), /bugs/{id}/comments,
   /comments/{id}, /activity, /sync/state, /projects, /health. ProblemDetail error handler. CORS from env.
6. FingerprintService with the §6.2 canonical format + unit test proving SHA-256 of bug 104's canonical string
   equals the seeded fingerprint.
7. Tests (JUnit + MockMvc, Testcontainers MySQL if Docker is available, otherwise a local test DB):
   PATCH status works, invalid enum → 400, unauthenticated → 401, activity_log row written with source=API.

Acceptance checks (run them, paste output in the walkthrough)
P1-1 ./mvnw test is green; app starts; Flyway reports V1–V3 applied.
P1-2 In MySQL CLI: SHOW TRIGGERS FROM bugdna shows 6 triggers; SELECT * FROM bug_summary returns 6 rows.
P1-3 curl login → token; curl PATCH /api/v1/bugs/104 {"status":"IN_PROGRESS"} → MySQL CLI SELECT shows IN_PROGRESS,
     activity_log newest row source='API'.
P1-4 In MySQL CLI: UPDATE bugs SET status='RESOLVED' WHERE id=104; → GET /api/v1/activity shows source='MYSQL_CLI';
     GET /api/v1/sync/state maxActivityId increased.
P1-5 INSERT and DELETE a comment in CLI → two activity rows (INSERT, DELETE) with source MYSQL_CLI.
P1-6 No secret values in git (git grep for the key names shows only .env.example placeholders).
Stop after the checks and wait for me.
```

### PHASE 2 — Premium dark dashboard with two-way sync and live feed

```text
PHASE 2 ONLY: dashboard/ (React + Vite + JavaScript + Tailwind + React Router), against the Phase 1 API.
Before any UI code: invoke the premium-component skill (it reads .agents/rules/premium-frontend.md and
references.md), then read .agents/rules/bugdna-app-ui.md and the app-screen guidance in the
design-taste-frontend skill. Write the 6-line brief from premium-frontend §0 for the concept in §7
("every bug has a genome"). If a skill step needs a script/reference file that is not present, say so in one
line and follow the SKILL.md text.

Build
1. tokens.css with the §7 palette/type/easing tokens (CSS variables), Geist + Geist Mono self-hosted.
2. Login page (dev login) → token in sessionStorage; api client adds Bearer and redirects to /login on 401.
3. Bugs page: dense table (title, DNA strand strip, status, priority, assignee, occurrences, last seen,
   REGRESSION tag), URL-synced filters + search, sort; right rail "Live Activity" feed showing source badges
   API / TRACKER / MYSQL_CLI with relative times.
4. Bug detail page: header with full 64-cell DNA strand + fingerprint (mono, copy button), inline editable
   status / priority / assignee (Radix or Base UI Select), comments (add/edit/delete; delete uses AlertDialog),
   placeholders for Timeline and AI panels (filled in Phases 4–5).
5. useSyncState hook per §6.5: 5 s setTimeout polling of /sync/state, pause when hidden, refetch on change,
   changed-row highlight, "Synced Ns ago" indicator, conflict note when a field is being edited.
6. Empty, loading (skeleton) and error states for every view.
Then run /baseline-ui and /fixing-motion-performance on the changed files and the impeccable critique once;
apply fixes.

Acceptance checks
P2-1 npm run build passes; no console errors; Lighthouse accessibility ≥ 90 on Bugs page.
P2-2 Change status of bug 104 in the UI → MySQL CLI SELECT shows the new value immediately.
P2-3 Run UPDATE bugs SET status='RESOLVED' WHERE id=104; in CLI → UI row updates within ≤ 5 s without reload,
     row highlights, feed shows "MYSQL_CLI · Bug #104: status … -> RESOLVED".
P2-4 INSERT a comment for 104 in CLI → appears in detail view and feed ≤ 5 s; DELETE it → disappears ≤ 5 s.
P2-5 DELETE FROM bugs WHERE id=106; in CLI → row disappears from list ≤ 5 s; open detail of 106 shows a
     "deleted in MySQL" state, not a crash.
P2-6 Keyboard-only: tab through filters, table, selects, dialog; focus is always visible.
P2-7 No banned patterns (Inter/Roboto/Poppins, purple gradients, glass cards, emoji icons, icon-title-text cards).
Stop and wait.
```

### PHASE 3 — Demo Store + reusable JavaScript tracker

```text
PHASE 3 ONLY: tracker/bugdna-tracker.js and demo-store/ (React + Vite, port 5174), plus /demo-api endpoints
in the backend (DemoStoreController) per §5. Ingest endpoint may be a stub that just stores events in this
phase (grouping comes in Phase 4) — but implement full event persistence + eventId dedupe now.

Tracker: implement §6.1 exactly — init API, sessionId, eventId UUIDs, PAGE_VIEW/ROUTE_CHANGE, CLICK only on
data-bugdna-action, JS_ERROR, PROMISE_REJECTION, fetch wrapper that CHECKS response.ok (fetch does not
reject on HTTP 500) and rethrows network errors, never tracks its own endpoint, browser-side sanitization,
ring buffer 30, immediate flush on error, keepalive send, sessionStorage retry with backoff.
Add tracker/README.md (how to embed in any app in 3 lines) and unit tests (Vitest) for sanitize() and the
fetch wrapper (mock a 500 → API_ERROR recorded and response still returned to caller).

Demo Store: Home (product grid), Product/:id, Cart (coupon field, button data-bugdna-action="APPLY_COUPON"),
Checkout (button data-bugdna-action="CHECKOUT"). Intentional bugs, each behind a clearly labeled
"Demo faults" panel for the presenter:
  a) coupon SAVE20 → checkout POST returns 500 DEMO_COUPON_RULE_ERROR (the main demo),
  b) "Track order" button throws TypeError reading 'price' of undefined,
  c) product id 999 → 404,
  d) "Sync wishlist" → unhandled promise rejection.
The store shows a friendly error to the shopper (it is a real-looking shop, not a test page). Style it as a
clean small shop in the same type family; it must not look like the dashboard.

Acceptance checks
P3-1 Vitest green; npm run build green for demo-store.
P3-2 Apply SAVE20 then Checkout → MySQL: SELECT … FROM events ORDER BY id DESC shows CLICK APPLY_COUPON on /cart,
     then API_ERROR POST 500 DEMO_COUPON_RULE_ERROR on /checkout, same session.
P3-3 Replay the same batch (curl with identical eventIds) → response duplicates = N, no new event rows.
P3-4 Type an email into any input, then trigger an error → no input value or email appears in events.
P3-5 Wrong X-BugDNA-Key → 401. Tracker never records its own /ingest calls.
Stop and wait.
```

### PHASE 4 — Fingerprinting, grouping, regression detection, timeline

```text
PHASE 4 ONLY: complete IngestService per §6.3 and the timeline per §5/§6.

1. FingerprintService normalization + canonical strings (§6.2) with table-driven unit tests: same error with
   different numeric ids / query strings → same fingerprint; different errorCode → different fingerprint.
2. Grouping with SELECT … FOR UPDATE + UNIQUE(project_id,fingerprint) race handling; occurrences rows;
   events.bug_id link; SET @bugdna_actor='TRACKER' for the batch (reset in finally).
3. Regression detection: RESOLVED/CLOSED bug that recurs → REOPENED, is_regression=1, regression_count+1,
   resolved_at NULL, occurrence was_regression=1; IGNORED stays ignored. The trigger logs "REGRESSION detected".
4. GET /bugs/{id}/occurrences and /bugs/{id}/timeline?occurrenceId= (same session, −120 s … +10 s, max 30).
5. Dashboard: Timeline panel (vertical, mono timestamps, relative offsets like "−8.2 s", trigger event
   emphasized, occurrence switcher, sessions-affected count); REGRESSION tag + red mutation tick on the DNA strand;
   strand pulses once when occurrence_count rises (reduced-motion: no pulse). Use the animate skill for the
   pulse and feed entry; check with /fixing-motion-performance.
6. Concurrency test: 20 parallel ingests of the same new error → exactly 1 bug, occurrence_count = 20.

Acceptance checks
P4-1 SAVE20 checkout (first time) → new bug (id ≥ 200) appears on the dashboard ≤ 5 s, feed "TRACKER · Bug #… created".
P4-2 Repeat 2× in the same session → same bug, occurrence_count 3; SQL from demo_queries §7 returns 0 rows.
P4-3 Repeat in a private window (new session) → sessions_affected = 2 in bug_summary and in the UI.
P4-4 Timeline for the latest occurrence shows PAGE_VIEW → CLICK APPLY_COUPON → ROUTE_CHANGE /checkout → CLICK CHECKOUT
     → API_ERROR 500, in order.
P4-5 TypeError fault → a different bug with a different fingerprint.
P4-6 Mark the SAVE20 bug RESOLVED (UI or CLI), trigger it again → status REOPENED, REGRESSION tag, regression_count 1,
     feed entry, CLI SELECT confirms.
P4-7 Concurrency test green.
Stop and wait.
```

### PHASE 5 — AI-assisted analysis (evidence vs hypothesis)

```text
PHASE 5 ONLY: AI analysis per §6.4.

1. AiProvider interface; GeminiProvider (REST generateContent, API key in x-goog-api-key header, JSON response
   schema), OpenAiProvider (structured JSON output), MockAiProvider. Select by AI_PROVIDER; model from AI_MODEL;
   keys only from env; 20 s timeout; on failure store status FAILED with a safe error message, never the key.
2. Build the prompt only from sanitized data (§6.4 input list). Add a unit test that the prompt contains no
   email, token, session id or user agent even when the stored events contained such patterns.
3. Validate output: drop evidence whose eventIds are not in the input timeline; cache by input_hash; force=true reruns.
4. POST/GET /bugs/{id}/analysis.
5. Dashboard AI panel: "Analyze with AI" button (loading spinner keeps label), sections Summary,
   Evidence (chips that scroll to + highlight the timeline event), Hypotheses (confidence badges, labeled
   "AI guesses — verify"), Debugging steps, Reproduction steps (numbered, copy as Markdown), provider/model/time
   footer, disclaimer. Works in mock mode with a visible "MOCK" badge.

Acceptance checks
P5-1 With a real key: analysis of the SAVE20 bug cites the APPLY_COUPON click and the 500 API_ERROR event ids as
     evidence and lists the coupon rule as a hypothesis, not a fact.
P5-2 Without a key (AI_PROVIDER=mock) the panel still works and shows MOCK.
P5-3 Second click without changes returns the cached row (no provider call — check logs).
P5-4 Sanitization test green; grep logs for the key → nothing.
Stop and wait.
```

### PHASE 6 — Analytics, polish, tests, docs, demo readiness

```text
PHASE 6 ONLY: finish and harden.

1. Analytics page from GET /analytics/overview (backed by the bug_summary VIEW): status/priority breakdown,
   errors per day (14 days, small SVG bars — no rainbow charts), top 5 bugs by occurrences, regressions count,
   mean minutes-to-resolve. Numbers must add up with the CLI queries.
2. Run the full impeccable critique + /baseline-ui + /fixing-motion-performance on dashboard and demo-store;
   fix all MUST violations from web-interface-guidelines. Reduced-motion pass.
3. Implement and run all 12 test cases from §9 (automate what you can: JUnit/MockMvc, Vitest; the rest as a
   manual checklist in docs/TEST_REPORT.md with results).
4. docs/DEMO_SCRIPT.md (the 10 scenes in §10), docs/VIVA.md (§12 answers incl. "why not Sentry"),
   docs/ER.md (Mermaid ER diagram of the 8 tables + view), root README.md (setup in ≤ 10 commands, env vars,
   ports, how to reset the demo: a database/reset_demo.sql that deletes bugs ≥ 200 and their events and resets
   seed bug 104 to OPEN).
5. Optional docker-compose.yml for MySQL 8.4 if a member has no local MySQL.

Acceptance checks
P6-1 Fresh clone → README steps → full demo works end to end in under 10 minutes.
P6-2 All automated tests green; TEST_REPORT.md lists TC-01…TC-12 with pass evidence.
P6-3 Analytics totals equal SELECT COUNT(*) … GROUP BY status in the CLI.
P6-4 The 10-scene demo runs twice in a row after reset_demo.sql without manual fixes.
Stop and wait.
```

---

## 9. Test cases (12)

| ID | Test | Steps | Expected |
|---|---|---|---|
| TC-01 | Dashboard → MySQL | Change bug 104 priority to HIGH in the UI, then `SELECT` in the CLI | The CLI shows HIGH. The feed shows an API entry. |
| TC-02 | MySQL → Dashboard | `UPDATE bugs SET status='RESOLVED' WHERE id=104;` | The UI updates within 5 s with no reload. The feed shows MYSQL_CLI. |
| TC-03 | CLI comment insert | `INSERT INTO comments …` for bug 104 | The comment and a feed entry appear within 5 s. |
| TC-04 | CLI delete | Delete that comment, then delete bug 106 | Both disappear within 5 s. The feed shows DELETE rows. The detail view shows a "deleted" state. |
| TC-05 | Automatic bug creation | Apply SAVE20 in the demo store, then check out | A new bug appears: `POST … 500 DEMO_COUPON_RULE_ERROR`. |
| TC-06 | Fingerprint grouping | Repeat TC-05 twice | Still one bug, with `occurrence_count` 3 and no duplicate fingerprint rows. |
| TC-07 | Cross-session impact | Repeat in a new private window | Sessions affected = 2. |
| TC-08 | Idempotent ingest | Re-POST an identical batch | `duplicates` = batch size and no new rows. |
| TC-09 | Distinct errors | Trigger the TypeError fault | A separate bug with a different fingerprint. |
| TC-10 | Regression | Resolve the SAVE20 bug, then trigger it again | REOPENED, a REGRESSION flag, `regression_count` 1 and a feed entry. |
| TC-11 | AI analysis | Analyze the SAVE20 bug, with and without a key | Evidence cites real event ids, hypotheses are labeled, and mock mode works. |
| TC-12 | Privacy and security | Type an email in an input and trigger an error; use a bad key; call the API without login | No PII is stored, the bad key gets 401, and no login gets 401. |

---

## 10. Demo script (10 scenes, about 8 minutes)

1. **The problem (30 s):** "A user says 'checkout isn't working.' Which page? What did they click? Which API? Did it hit anyone else?" Show the empty-ish dashboard (seed data) and its dark UI.
2. **Architecture (45 s):** walk through the §2 diagram: tracker → Spring Boot → MySQL → dashboard, plus the AI.
3. **Break it live (45 s):** in the demo store, add an item, apply **SAVE20** (it works), then click Checkout. The shopper sees an error.
4. **Bug appears by itself (30 s):** within 5 s the dashboard shows a new bug, `POST /demo-api/checkout → 500 DEMO_COUPON_RULE_ERROR`, with a TRACKER entry in the feed.
5. **Timeline (60 s):** open the bug and show the click-by-click lead-up: page view, APPLY_COUPON click, route to checkout, CHECKOUT click, 500. "The user never had to describe any of this."
6. **DNA fingerprint grouping (45 s):** repeat the checkout twice and once in a new private window. The occurrence count reaches 4, sessions affected reaches 2, and the strand pulses. In the CLI, run demo query §7, which returns 0 duplicate rows thanks to the unique key.
7. **AI analysis (60 s):** click Analyze. Evidence chips jump to the actual events, and the hypotheses are labeled as guesses with confidence. Copy the reproduction steps.
8. **Teacher's MySQL CLI, both ways (90 s):** the teacher runs `UPDATE bugs SET status='RESOLVED' WHERE id=104;` and the row flips within 5 s, with a MYSQL_CLI feed entry. Next they insert a comment and delete it. Then change priority in the UI and `SELECT` in the CLI. Finally, run `SHOW TRIGGERS` to show how the feed knows.
9. **Regression (45 s):** mark the SAVE20 bug Resolved, then break checkout again. It auto-reopens with a red REGRESSION mutation mark.
10. **Analytics and wrap-up (45 s):** the analytics page (from the `bug_summary` view), then the "why not Sentry" line, privacy rules, and what we'd add next (session replay with rrweb, Slack alerts, source maps).

---

## 11. Three-member split

| Member | Owns | Phases |
|---|---|---|
| **Member 1: Backend and database** (e.g. Aarav) | Schema, Flyway, triggers, view, seed, ingest pipeline, fingerprinting, regression, concurrency test, `demo_queries.sql` | 1, 4 (lead) |
| **Member 2: Dashboard UI** | Design brief with the kit skills, tokens, Bugs, Detail, Timeline and Analytics pages, sync hook, live feed, accessibility and motion passes | 2, 4 (UI), 6 (analytics and polish) |
| **Member 3: Tracker, store, AI and docs** | `bugdna-tracker.js`, demo store and faults, `/demo-api`, AI providers and panel, test report, README, demo script and viva doc | 3, 5, 6 (docs) |

Shared rules: one Git repo, a branch per phase, a PR reviewed by one other member, and everyone able to run the full demo. In the viva, each member explains one other member's module too.

---

## 12. Viva answers

- **Why not just use Sentry?** *"Sentry is a full production observability platform. BugDNA is a focused, self-hosted, privacy-first version we built end to end to show the internals Sentry hides: deterministic SHA-256 fingerprinting enforced by a MySQL unique key, a click-to-crash timeline, AI reproduction steps that must cite recorded events, and trigger-driven two-way sync with the database."*
- **Why polling, not WebSockets?** It's simpler and stateless, and changes made directly in MySQL never pass through the app, so a push channel couldn't see them anyway. The `activity_log` triggers plus a cheap `/sync/state` poll catch every change within 5 s.
- **How do you know a change came from the CLI?** Every app connection runs `SET @bugdna_actor='API'` (Hikari init SQL) and the ingest transaction sets `'TRACKER'`. A CLI session has no variable, so the trigger's `COALESCE` labels it `MYSQL_CLI`.
- **Why can't the AI group bugs?** Grouping must be deterministic, explainable and testable. The AI only explains, and its evidence is validated against real event ids.
- **Why check `response.ok`?** `fetch()` only rejects on network failure. A 500 still resolves, so without the check, API failures would be invisible.
- **What stops duplicate events?** Client UUID `eventId` plus `UNIQUE(project_id, event_id)`, so retries are idempotent.
- **What don't you collect?** Input values, cookies, auth headers, bodies and query strings. Emails, tokens and long numbers are masked, following the OWASP Logging Cheat Sheet.

**References:** MDN Fetch API (HTTP errors don't reject), Spring Boot servlet and error-handling docs, OWASP Logging Cheat Sheet, and rrweb docs (future session replay, not in the MVP).
