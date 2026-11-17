# BugDNA — Master Test Report (TC-01 … TC-12)

**Test Suites Executed:**
- **Backend (JUnit 5 + MockMvc + Spring Boot Integration):** `./mvnw test` → **28 passed, 0 failed, 0 errors**
- **Tracker (Vitest):** `cd tracker && npm test` → **7 passed, 0 failed**
- **Dashboard Build:** `cd dashboard && npm run build` → **0 errors**
- **Demo Store Build:** `cd demo-store && npm run build` → **0 errors**

---

## Verification Matrix (TC-01 through TC-12)

| ID | Test Case | Automated / Live Evidence | Status |
|---|---|---|---|
| **TC-01** | **Dashboard → MySQL** (Change bug 104 priority/status in UI → CLI shows updated value + `API` feed row) | Verified via `BugControllerTest` + live `PATCH /api/v1/bugs/104 {"status":"IN_PROGRESS"}` → MySQL `SELECT` confirmed `IN_PROGRESS` and `activity_log` row #12 logged `source='API'`. | **PASS** |
| **TC-02** | **MySQL → Dashboard** (`UPDATE bugs SET status='RESOLVED' WHERE id=104;` → UI updates ≤ 5 s + `MYSQL_CLI` feed row) | Verified live: MySQL CLI `UPDATE` fired `trg_bugs_au`, logged `activity_log` row #13 with `source='MYSQL_CLI'`, incremented `/sync/state.maxActivityId`, triggering `useSyncState` refetch and row highlight. | **PASS** |
| **TC-03** | **CLI comment insert** (`INSERT INTO comments …` for bug 104 → appears in detail view & feed ≤ 5 s) | Verified live: MySQL CLI `INSERT` fired `trg_comments_ai`, logged `activity_log` row #14 (`COMMENT INSERT MYSQL_CLI`) and bumped `/sync/state.commentCount`. | **PASS** |
| **TC-04** | **CLI delete** (Delete comment & delete bug 106 → both disappear ≤ 5 s; detail shows "deleted in MySQL" state) | Verified live: CLI `DELETE` fired `trg_comments_ad` (`activity_log` row #15) and `trg_bugs_ad`; `BugDetailPage.jsx` handles HTTP 404 with a dedicated non-crashing "Bug #… has been deleted from MySQL" screen. | **PASS** |
| **TC-05** | **Automatic bug creation** (Apply `SAVE20` in demo store, then checkout → new bug `POST … 500 DEMO_COUPON_RULE_ERROR`) | Verified live: `/demo-api/cart/coupon` returned 200, `/demo-api/checkout` returned 500 `DEMO_COUPON_RULE_ERROR`, ingest created **Bug #200** (`id ≥ 200`) with `[TRACKER]` activity log entry. | **PASS** |
| **TC-06** | **Fingerprint grouping** (Repeat TC-05 twice → 1 bug, `occurrence_count = 3`, 0 duplicate fingerprints) | Verified live + `FingerprintServiceTest` (6 tests) + `IngestConcurrencyAndRegressionTest` (20 parallel threads → 1 bug, `occurrence_count = 20`); MySQL `HAVING COUNT(*) > 1` returned **0 rows**. | **PASS** |
| **TC-07** | **Cross-session impact** (Repeat in a second session → `sessions_affected = 2`) | Verified live: second session `p4-live-sess-2` bumped `bug_summary.sessions_affected` and `GET /api/v1/bugs/200.sessionsAffected` to **2**. | **PASS** |
| **TC-08** | **Idempotent ingest** (Re-POST identical batch with same `eventIds` → `duplicates = N`, 0 new rows) | Verified live: 1st POST returned `{"accepted":2,"duplicates":0}`; replay POST returned `{"accepted":0,"duplicates":2}` with zero new rows in `events`. | **PASS** |
| **TC-09** | **Distinct errors** (Trigger `TypeError` fault → separate bug with different fingerprint) | Verified live: `JS_ERROR TypeError` created distinct **Bug #201** with a separate SHA-256 fingerprint from Bug #200. | **PASS** |
| **TC-10** | **Regression** (Resolve SAVE20 bug, trigger again → `REOPENED`, `REGRESSION` flag, `regression_count = 1`, feed entry) | Verified live + `IngestConcurrencyAndRegressionTest`: `status` flipped `RESOLVED → REOPENED`, `isRegression = true`, `regressionCount = 1`, `activity_log` logged `[TRACKER] Bug #200: status RESOLVED -> REOPENED; REGRESSION detected`. | **PASS** |
| **TC-11** | **AI analysis** (Analyze SAVE20 bug → Evidence cites real event IDs, Hypotheses labeled, Mock mode & caching work) | Verified live + `AiAnalysisServiceTest` (3 tests): `POST /api/v1/bugs/200/analysis` returned `MOCK`, Evidence cited real `eventId`, Hypotheses labeled with confidence, 2nd call served from cache (`cached=true`), hallucinated IDs dropped. | **PASS** |
| **TC-12** | **Privacy & security** (Email entered → no PII stored; bad `X-BugDNA-Key` → 401; no Bearer token → 401) | Verified live + `SanitizerTest` + `AiAnalysisServiceTest` + `AuthControllerTest` + `IngestControllerTest` + Tracker Vitest (7 tests): emails/tokens/digit runs masked to `[email]`/`[token]`/`[number]`, bad key → 401, unauthenticated → 401, zero keys in git or logs. | **PASS** |

