# BugDNA — Live Presentation Demo Script (10 Scenes, ~8 Minutes)

**Pre-flight Reset (run before walking up to present):**
```powershell
mysql -u bugdna -pCHANGE_ME_LOCAL_ONLY < database/reset_demo.sql
```
Open three windows side-by-side:
1. **BugDNA Dashboard** (`http://localhost:5173`, logged in as `dev` / `password123`)
2. **Kova Goods Demo Store** (`http://localhost:5174`)
3. **MySQL Command Line Client** (`USE bugdna;`)

---

### Scene 1 — The Problem (30 s)
- **Say:** *"A shopper sends a support ticket: 'Checkout isn't working.' Which page were they on? What did they click? Which API returned 500? Did anyone else hit it?"*
- **Show:** BugDNA Dashboard (`http://localhost:5173/bugs`) with the 6 seeded bugs (`#101–#106`), dark developer workbench UI, 64-cell DNA strands, and Live Activity Rail.

### Scene 2 — Architecture Walkthrough (45 s)
- **Explain:** Four-part loop:
  1. Zero-dependency JS tracker (`tracker/bugdna-tracker.js`) embedded in Kova Goods (`:5174`).
  2. Spring Boot API (`:8080`) with server-side sanitization, deterministic SHA-256 fingerprinting, and concurrency-safe grouping.
  3. MySQL 8 database with `UNIQUE(project_id, fingerprint)`, 6 triggers writing `activity_log`, and `VIEW bug_summary`.
  4. React Dashboard (`:5173`) polling `/api/v1/sync/state` every 5 seconds for live two-way sync.

### Scene 3 — Break It Live (45 s)
- **Do (Demo Store `:5174`):**
  1. Add an item to the bag or go to **Cart** (`/cart`).
  2. Enter promo code **`SAVE20`** and click **Apply** (`data-bugdna-action="APPLY_COUPON"`). Notice it succeeds with **200 OK** (20% discount applied — the bug hides until checkout).
  3. Click **Proceed to Checkout**, fill in an email (e.g. `shopper@example.com`) and name, and click **Place Order** (`data-bugdna-action="CHECKOUT"`).
- **Show:** The shopper sees a polite error banner (never a raw stack trace).

### Scene 4 — Bug Appears Automatically (30 s)
- **Switch to Dashboard (`:5173/bugs`):** Within ≤ 5 seconds (no page reload), **Bug #200** (`POST /demo-api/checkout → 500 DEMO_COUPON_RULE_ERROR`) appears at the top of the table, its DNA strand pulses once, and the right rail logs **`TRACKER · Bug #200 created`**.

### Scene 5 — Click-to-Crash Timeline (60 s)
- **Do:** Click **Bug #200** to open its detail view.
- **Show (Left Column):** The vertical **Click-to-Crash Timeline** with negative relative offsets (`−9.0 s → −7.0 s → −4.0 s → −1.0 s → 0.0 s`):
  `PAGE_VIEW /cart → CLICK APPLY_COUPON → ROUTE_CHANGE /checkout → CLICK CHECKOUT → API_ERROR 500 DEMO_COUPON_RULE_ERROR [TRIGGER]`.
- **Say:** *"The user never had to describe a single step — and notice their email address was never collected."*

### Scene 6 — DNA Fingerprint Grouping & Cross-Session Impact (45 s)
- **Do:**
  1. Click **Place Order** two more times in the Demo Store (`occurrence_count` rises to **3**).
  2. Open an **Incognito/Private window** to `http://localhost:5174`, apply `SAVE20`, and click **Place Order** once more.
- **Show:** On the Dashboard, `occurrence_count` reaches **4**, `sessions_affected` reaches **2**, and the 64-cell DNA strand pulses.
- **Run in MySQL CLI:**
  ```sql
  SELECT project_id, fingerprint, COUNT(*) FROM bugs GROUP BY project_id, fingerprint HAVING COUNT(*) > 1;
  ```
  Returns **0 rows** — enforced by `UNIQUE(project_id, fingerprint)` and `SELECT ... FOR UPDATE`.

### Scene 7 — Grounded AI Analysis (60 s)
- **Do:** On Bug #200 detail, click **Analyze with AI**.
- **Show:**
  - **Evidence (from recorded events):** Click a cited `eventId` chip — the page scrolls to and highlights the exact node in the timeline.
  - **Hypotheses (AI guesses — verify):** Clearly separated with confidence badges (`HIGH` / `MEDIUM`) and disclaimer.
  - Click **Copy as Markdown** on the numbered reproduction steps.
  - Click **Analyze with AI** a second time — returns instantly with a **`CACHED`** badge (0 extra provider calls).

### Scene 8 — Teacher's MySQL CLI Two-Way Sync (90 s)
- **Do (Teacher runs in MySQL CLI from `database/demo_queries.sql`):**
  1. `UPDATE bugs SET status = 'RESOLVED' WHERE id = 104;` → Within 5 s, Dashboard flips Bug #104 to `RESOLVED`, flashes the row, and logs **`MYSQL_CLI · Bug #104: status OPEN -> RESOLVED`**.
  2. `INSERT INTO comments (bug_id, author, body) VALUES (104, 'Teacher (CLI)', 'Checked from MySQL CLI - please verify the fix.');` → Appears under Bug #104 and in the Live Activity Rail.
  3. `DELETE FROM comments WHERE bug_id = 104 AND author = 'Teacher (CLI)';` → Disappears within 5 s and logs `DELETE`.
  4. Change Bug #104 Priority to **HIGH** in the Dashboard UI → `SELECT id, status, priority FROM bugs WHERE id = 104;` immediately reflects `HIGH` with `source = 'API'`.
  5. `SHOW TRIGGERS FROM bugdna;` → Show all 6 triggers and `COALESCE(@bugdna_actor, 'MYSQL_CLI')`.

### Scene 9 — Automatic Regression Detection (45 s)
- **Do:** Mark **Bug #200** (`SAVE20`) as **`RESOLVED`** in the Dashboard (or CLI). Then click **Place Order** with `SAVE20` again in the Demo Store.
- **Show:** Within 5 s, Bug #200 automatically flips from `RESOLVED` → **`REOPENED`**, displays the red **`REGRESSION`** badge, marks red mutation ticks on its 64-cell DNA strand, increments `regression_count = 1`, and logs **`TRACKER · Bug #200: status RESOLVED -> REOPENED; REGRESSION detected`**.

### Scene 10 — Analytics & Wrap-Up (45 s)
- **Show:** Open **Analytics** (`/analytics`), backed directly by `VIEW bug_summary`: totals by status and priority, 14-day SVG error bar chart, regressions flagged, mean time to resolve, and Top 5 bugs by occurrences.
- **Wrap up:** State the "why not Sentry?" answer, OWASP privacy guarantees, and future roadmap (rrweb session replay, source maps, Slack webhooks).

