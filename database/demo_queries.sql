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
