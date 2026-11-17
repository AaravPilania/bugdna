-- BugDNA Demo Reset Script
-- Resets the database to a clean pre-demo state so the 10-scene presentation can be
-- rehearsed or run back-to-back without manual fixes.

USE bugdna;

SET @bugdna_actor = 'SEED';

-- 1. Remove live demo bugs (>= 200); cascades to bug_occurrences, comments, and ai_analyses
DELETE FROM bugs WHERE id >= 200;

-- 2. Remove live demo events and sessions (preserving seed events 1..15 and seed sessions 1..2)
DELETE FROM events WHERE id > 15;
DELETE FROM tracking_sessions WHERE id > 2;

-- 3. Remove any teacher CLI comments added on seeded bugs during previous runs
DELETE FROM comments WHERE id > 5;

-- 4. Remove live activity log entries generated after seed
DELETE FROM activity_log WHERE source <> 'SEED';

-- 5. Reset seed bug 104 back to OPEN / MEDIUM / unassigned for the teacher CLI demo
UPDATE bugs
SET status = 'OPEN',
    priority = 'MEDIUM',
    assignee = NULL,
    resolved_at = NULL,
    is_regression = 0,
    regression_count = 0
WHERE id = 104;

-- 6. Re-seed bug 106 if it was deleted in Teacher Demo Query #10
INSERT IGNORE INTO bugs (
  id, project_id, fingerprint, title, error_type, page_path,
  status, priority, assignee, occurrence_count, is_regression, regression_count,
  first_seen_at, last_seen_at, created_at, updated_at
)
VALUES (
  106, 1,
  SHA2('v1|JS_ERROR|ReferenceError|inventorystock is not defined|cart.js:validatecheckout', 256),
  'ReferenceError: inventoryStock is not defined',
  'JS_ERROR', '/cart',
  'REOPENED', 'CRITICAL', 'Member 2', 5, 1, 1,
  DATE_SUB(NOW(3), INTERVAL 120 MINUTE), DATE_SUB(NOW(3), INTERVAL 5 MINUTE),
  DATE_SUB(NOW(3), INTERVAL 120 MINUTE), DATE_SUB(NOW(3), INTERVAL 5 MINUTE)
);

-- 7. Ensure the next live-created bug gets id = 200
ALTER TABLE bugs AUTO_INCREMENT = 200;

SET @bugdna_actor = 'API';

SELECT id, title, status, priority, occurrence_count FROM bug_summary ORDER BY id ASC;

