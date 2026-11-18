# BugDNA — Contextual Error Tracking & AI-Assisted Debugging Platform

> **Core Concept:** *"Every bug has a genome."*
> Deterministic SHA-256 error fingerprinting enforced by MySQL, click-to-crash session timelines, evidence-grounded AI debugging analysis, and trigger-backed two-way database synchronization.

---

## Architecture & Ports

| Service | Stack | Port | Purpose |
|---|---|---|---|
| **Backend API** (`backend/`) | Spring Boot 4.1 (Java 21+), JPA, Flyway, MySQL 8 | `:8080` | REST `/api/v1/**`, ingest, fingerprinting, AI analysis, `/demo-api/**` |
| **Dashboard** (`dashboard/`) | React + Vite (JS), Tailwind CSS, React Router | `:5173` | Dark developer workbench, 64-cell DNA strands, 5s sync, Live Activity Rail |
| **Demo Store** (`demo-store/`) | React + Vite (JS), Tailwind CSS | `:5174` | Kova Goods retail shop embedding `tracker/bugdna-tracker.js` + presenter faults |
| **Tracker** (`tracker/`) | Zero-dependency ES Module (`bugdna-tracker.js`) | — | Reusable browser telemetry module with `fetch` wrapper & PII masking |
| **MySQL 8** | MySQL 8.0 / 8.4 (local or `docker-compose.yml`) | `:3306` | 8 tables, 6 triggers (`activity_log`), `VIEW bug_summary` |

---

## Quick Start

### Option A: One-Command Windows Launcher (PowerShell)
```powershell
# 1. Start all 3 servers in dedicated windows and wait for health check:
.\start-demo.ps1

# 2. Reset database between demo runs anytime:
.\reset-demo.ps1
```

### Option B: Manual Multi-Platform Setup
**Prerequisites:** JDK 21+, Node.js 20+, MySQL 8.0/8.4 (or Docker Desktop).

```bash
# 1. Start MySQL (either local service + setup.sql OR docker compose up -d)
# In Linux/macOS bash:
mysql -u root -p < database/setup.sql
# In Windows PowerShell:
Get-Content database/setup.sql | mysql -u root -p
# Or Docker:
docker compose up -d

# 2. Configure local env files (optional: defaults work out-of-the-box for local dev)
cp backend/.env.example backend/.env
cp dashboard/.env.example dashboard/.env
cp demo-store/.env.example demo-store/.env

# 3. Run backend tests & start Spring Boot on :8080 (applies Flyway V1–V3 automatically)
cd backend && ./mvnw test && ./mvnw spring-boot:run

# 4. In terminal 2 — install & start BugDNA Dashboard on :5173
cd dashboard && npm install && npm run dev

# 5. In terminal 3 — install & start Kova Goods Demo Store on :5174
cd demo-store && npm install && npm run dev

# 6. Run tracker unit tests (Vitest)
cd tracker && npm install && npm test
```

Open `http://localhost:5173` and sign in with the dev credentials (`dev` / `password123`).

---

## Environment Variables

| Variable | App | Default / Example | Purpose |
|---|---|---|---|
| `DB_URL` | `backend` | `jdbc:mysql://localhost:3306/bugdna?...` | MySQL JDBC URL |
| `DB_USER` | `backend` | `bugdna` | Database username |
| `DB_PASSWORD` | `backend` | `CHANGE_ME_LOCAL_ONLY` | Database password (never commit real secrets) |
| `BUGDNA_DEV_USER` | `backend` | `dev` | Dashboard dev login username |
| `BUGDNA_DEV_PASSWORD` | `backend` | `password123` | Dashboard dev login password |
| `BUGDNA_CORS_ORIGINS` | `backend` | `http://localhost:5173,http://localhost:5174` | Allowed frontend origins |
| `AI_PROVIDER` | `backend` | `mock` (`gemini` \| `openai` \| `mock`) | Active AI provider (falls back to `mock` if no key) |
| `AI_MODEL` | `backend` | `gemini-2.5-flash` | Provider model name |
| `GEMINI_API_KEY` | `backend` | *(empty)* | Gemini API key (sent strictly via `x-goog-api-key`) |
| `OPENAI_API_KEY` | `backend` | *(empty)* | OpenAI API key (sent strictly via `Authorization`) |
| `VITE_API_BASE` | `dashboard`, `demo-store` | `http://localhost:8080/api/v1` / `http://localhost:8080` | Backend base URL |
| `VITE_BUGDNA_PROJECT_KEY` | `demo-store` | `demo-store-dev-key` | Project key sent in `X-BugDNA-Key` |

---

## Resetting Between Demo Runs

Run `database/reset_demo.sql` to delete all live bugs (`id ≥ 200`), reset `AUTO_INCREMENT = 200`, and restore seed bug `#104` to `OPEN`:

```bash
mysql -u bugdna -pCHANGE_ME_LOCAL_ONLY < database/reset_demo.sql
```

---

## Documentation

- [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md) — 10-scene (~8 min) live presentation script
- [docs/VIVA.md](docs/VIVA.md) — Viva Q&A & technical defense guide (including *"Why not Sentry?"*)
- [docs/ER.md](docs/ER.md) — Mermaid ER diagram of all 8 tables, 6 triggers, and `bug_summary` view
- [docs/TEST_REPORT.md](docs/TEST_REPORT.md) — Complete TC-01…TC-12 verification report
- [database/demo_queries.sql](database/demo_queries.sql) — Teacher MySQL Command Line Client demo queries
- [tracker/README.md](tracker/README.md) — 3-line tracker embed guide

